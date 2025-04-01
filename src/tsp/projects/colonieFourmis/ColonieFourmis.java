package tsp.projects.colonieFourmis;

import tsp.evaluation.Coordinates;
import tsp.evaluation.Evaluation;
import tsp.evaluation.Path;
import tsp.projects.CompetitorProject;
import tsp.projects.InvalidProjectException;

import java.util.ArrayList;
import java.util.Random;

public class ColonieFourmis extends CompetitorProject {
    private static final int NB_FOURMIS = 15;
    private static final int NB_ITERATIONS = 100;
    private static final double TAUX_EVAPORATION = 0.35;
    private static final double Q = 100.0;
    private static final double PHEROMONE_INITIAL = 0.1;
    private static final double ALPHA = 1.2; // Poids des phéromones
    private static final double BETA = 5.0;  // Poids de la visibilité (1/distance)

    private Random random;
    private Evaluation evaluation;
    private double[][] pheromones;
    private double[][] distances;
    private Path meilleurChemin;
    private double meilleureDistance;

    public ColonieFourmis(Evaluation evaluation) throws InvalidProjectException {
        super(evaluation);
        this.addAuthor("Mohamed Krouchi");
        this.addAuthor("Emma Houver");
        this.setMethodName("Colonie de fourmis optimisée");
        this.evaluation = evaluation;
        this.random = new Random();
    }

    @Override
    public void initialization() {
        int nbVilles = this.problem.getLength();
        initialiserPheromones(nbVilles);
        calculerDistances(nbVilles);
        this.meilleurChemin = new Path(nbVilles);
        this.meilleureDistance = Double.MAX_VALUE;
    }

    private void initialiserPheromones(int nbVilles) {
        pheromones = new double[nbVilles][nbVilles];
        for (int i = 0; i < nbVilles; i++) {
            for (int j = 0; j < nbVilles; j++) {
                pheromones[i][j] = PHEROMONE_INITIAL;
            }
        }
    }

    private void calculerDistances(int nbVilles) {
        distances = new double[nbVilles][nbVilles];
        for (int i = 0; i < nbVilles; i++) {
            for (int j = 0; j < nbVilles; j++) {
                distances[i][j] = calculerDistance(i, j);
            }
        }
    }

    @Override
    public void loop() {
        long startTime = System.currentTimeMillis();
        long timeLimit = 70 * 1000; // 70 secondes en millisecondes

        int nbVilles = this.problem.getLength();

        for (int iteration = 0; iteration < NB_ITERATIONS; iteration++) {
            if (System.currentTimeMillis() - startTime >= timeLimit) {
                System.out.println("Temps écoulé : arrêt de l'algorithme.");
                break;
            }

            ArrayList<Path> cheminsFourmis = new ArrayList<>();
            ArrayList<Double> distancesFourmis = new ArrayList<>();

            for (int fourmi = 0; fourmi < NB_FOURMIS; fourmi++) {
                CheminEtDistance resultat = construireChemin(nbVilles);
                cheminsFourmis.add(resultat.chemin);
                distancesFourmis.add(resultat.distance);

                if (resultat.distance < this.meilleureDistance) {
                    this.meilleureDistance = resultat.distance;
                    this.meilleurChemin = resultat.chemin;
                    this.evaluation.evaluate(this.meilleurChemin);
                }
            }

            evaporerPheromones(nbVilles);
            deposerPheromones(cheminsFourmis, distancesFourmis);
        }

        this.evaluation.evaluate(this.meilleurChemin);
    }

    private CheminEtDistance construireChemin(int nbVilles) {
        int[] chemin = new int[nbVilles];
        boolean[] visite = new boolean[nbVilles];

        int villeCourante = random.nextInt(nbVilles);
        chemin[0] = villeCourante;
        visite[villeCourante] = true;

        for (int etape = 1; etape < nbVilles; etape++) {
            int villeSuivante = choisirVilleSuivante(villeCourante, visite);
            chemin[etape] = villeSuivante;
            visite[villeSuivante] = true;
            villeCourante = villeSuivante;
        }

        Path path = new Path(chemin);
        double distance = this.evaluation.quickEvaluate(path);
        Path cheminAmeliore = ameliorationLocale2Opt(path);
        double distanceAmelioree = this.evaluation.quickEvaluate(cheminAmeliore);

        return new CheminEtDistance(cheminAmeliore, distanceAmelioree);
    }

    private int choisirVilleSuivante(int villeCourante, boolean[] visite) {
        double sommeTotal = 0.0;
        double[] probabilites = new double[visite.length];

        // 1. Calcul du dénominateur (somme totale)
        for (int ville = 0; ville < visite.length; ville++) {
            if (!visite[ville]) {
                double pheromone = pheromones[villeCourante][ville];
                double distance = distances[villeCourante][ville];
                double visibilite = 1.0 / distance; // s[i][j]

                probabilites[ville] = Math.pow(pheromone, ALPHA) * Math.pow(visibilite, BETA);
                sommeTotal += probabilites[ville];
            }
        }

        // 2. Normalisation et sélection par roulette
        double randomValue = random.nextDouble() * sommeTotal;
        double cumulativeSum = 0.0;

        for (int ville = 0; ville < visite.length; ville++) {
            if (!visite[ville]) {
                cumulativeSum += probabilites[ville];
                if (cumulativeSum >= randomValue) {
                    return ville;
                }
            }
        }

        // Cas de secours (normalement inaccessible si le graphe est complet)
        for (int ville = 0; ville < visite.length; ville++) {
            if (!visite[ville]) return ville;
        }
        return -1; // Erreur
    }

    private Path ameliorationLocale2Opt(Path chemin) {
        int[] villes = chemin.getPath();
        boolean amelioration = true;

        while (amelioration) {
            amelioration = false;

            for (int i = 1; i < villes.length - 2; i++) {
                for (int j = i + 1; j < villes.length - 1; j++) {
                    if (calculerGain2Opt(villes, i, j) > 0) {
                        inverserSegment(villes, i, j);
                        amelioration = true;
                    }
                }
            }
        }

        return new Path(villes);
    }

    private double calculerGain2Opt(int[] villes, int i, int j) {
        int villeA = villes[i - 1], villeB = villes[i];
        int villeC = villes[j], villeD = villes[j + 1];

        double ancienneDistance = distances[villeA][villeB] + distances[villeC][villeD];
        double nouvelleDistance = distances[villeA][villeC] + distances[villeB][villeD];

        return ancienneDistance - nouvelleDistance;
    }

    private void inverserSegment(int[] villes, int i, int j) {
        while (i < j) {
            int temp = villes[i];
            villes[i] = villes[j];
            villes[j] = temp;
            i++;
            j--;
        }
    }

    private void evaporerPheromones(int nbVilles) {
        for (int i = 0; i < nbVilles; i++) {
            for (int j = 0; j < nbVilles; j++) {
                pheromones[i][j] *= (1.0 - TAUX_EVAPORATION);
            }
        }
    }

    private void deposerPheromones(ArrayList<Path> chemins, ArrayList<Double> distances) {
        for (int f = 0; f < chemins.size(); f++) {
            Path chemin = chemins.get(f);
            double distance = distances.get(f);
            int[] villes = chemin.getPath();
            double quantitePheromones = Q / distance;

            for (int i = 0; i < villes.length - 1; i++) {
                pheromones[villes[i]][villes[i + 1]] += quantitePheromones;
                pheromones[villes[i + 1]][villes[i]] += quantitePheromones;
            }
        }
    }

    private double calculerDistance(int ville1, int ville2) {
        Coordinates c1 = this.problem.getCoordinates(ville1);
        Coordinates c2 = this.problem.getCoordinates(ville2);
        return c1.distance(c2);
    }

    private class CheminEtDistance {
        Path chemin;
        double distance;

        public CheminEtDistance(Path chemin, double distance) {
            this.chemin = chemin;
            this.distance = distance;
        }
    }
}