package tsp.projects.colonieFourmis;

import tsp.evaluation.Coordinates;
import tsp.evaluation.Evaluation;
import tsp.evaluation.Path;
import tsp.projects.CompetitorProject;
import tsp.projects.InvalidProjectException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ColonieFourmis extends CompetitorProject {
    private static final double TAUX_EVAPORATION = 0.40;
    private static final double Q = 100.0;
    private static final double PHEROMONE_INITIAL = 0.22;
    private static final double ALPHA = 1.5; // Poids des phéromones
    private static final double BETA = 7.2;  // Poids de la visibilité (1/distance)
    private int nbVilles;

    private Random random;
    private Evaluation evaluation;
    private double[][] pheromones;
    private double[][] distances;
    private Path meilleurChemin;
    private double meilleureDistance;
    private final Set<Integer> villesDepartUtilisees = new HashSet<>();

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
        nbVilles = this.problem.getLength();
        initialiserPheromones(nbVilles);
        distances = new double[nbVilles][nbVilles];
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
        for (int i = 0; i < nbVilles; i++) {
            for (int j = 0; j < nbVilles; j++) {
                Coordinates c1 = this.problem.getCoordinates(i); //ville i
                Coordinates c2 = this.problem.getCoordinates(j); //ville j
                distances[i][j] = c1.distance(c2);
            }
        }
    }

    @Override
    public void loop() {
        calculerDistances(nbVilles);

        ArrayList<Path> cheminsFourmis = new ArrayList<>();
        ArrayList<Double> distancesFourmis = new ArrayList<>();

        int nb_fourmis = 35;

        for (int fourmi = 0; fourmi < nb_fourmis; fourmi++) {
            CheminEtDistance resultat = construireChemin(nbVilles);
            cheminsFourmis.add(resultat.chemin);
            distancesFourmis.add(resultat.distance);

            if (resultat.distance < this.meilleureDistance) {
                this.meilleureDistance = resultat.distance;
                this.meilleurChemin = resultat.chemin;
            }
            this.evaluation.evaluate(this.meilleurChemin);
            evaporerPheromones(nbVilles);
        }
    }

    private int choisirVilleDepartUnique(int nbVilles) {
        if (villesDepartUtilisees.size() >= nbVilles) {
            villesDepartUtilisees.clear();
        }

        int ville;
        do {
            ville = random.nextInt(nbVilles);
        } while (villesDepartUtilisees.contains(ville));

        villesDepartUtilisees.add(ville);
        return ville;
    }


    private CheminEtDistance construireChemin(int nbVilles) {
        int[] chemin = new int[nbVilles];
        boolean[] visite = new boolean[nbVilles];
        double distanceTotale = 0.0;

        // Ville de départ
        int villeCourante = choisirVilleDepartUnique(nbVilles);
        chemin[0] = villeCourante;
        visite[villeCourante] = true;

        // Construction progressive avec mise à jour en temps réel
        for (int etape = 1; etape < nbVilles; etape++) {
            int villeSuivante = choisirVilleSuivante(villeCourante, visite);
            chemin[etape] = villeSuivante;
            visite[villeSuivante] = true;

            // Mise à jour IMMÉDIATE des phéromones locales
            double distanceSegment = distances[villeCourante][villeSuivante];
            distanceTotale += distanceSegment;

            // Dépôt progressif (méthode "Pheromone Guided")
            double depot = (Q / nbVilles) / distanceSegment;
            pheromones[villeCourante][villeSuivante] =
                    Math.min(100.0, pheromones[villeCourante][villeSuivante] + depot);
            pheromones[villeSuivante][villeCourante] = pheromones[villeCourante][villeSuivante];

            villeCourante = villeSuivante;
        }

        // Optimisation et évaluation finale
        Path path = new Path(chemin);
        Path cheminAmeliore = ameliorationLocale2Opt(path);
        double distanceAmelioree = this.evaluation.quickEvaluate(cheminAmeliore);

        // Dépôt final supplémentaire pour le meilleur chemin
        double depotFinal = Q / distanceAmelioree;
        int[] villes = cheminAmeliore.getPath();
        for (int i = 0; i < villes.length - 1; i++) {
            pheromones[villes[i]][villes[i+1]] += depotFinal;
            pheromones[villes[i+1]][villes[i]] += depotFinal;
        }

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
/*
        for (int ville = 0; ville < visite.length; ville++) {
            if (!visite[ville]) {
                probabilites[ville] = probabilites[ville]/sommeTotal;
            }
        }
*/
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

    private class CheminEtDistance {
        Path chemin;
        double distance;

        public CheminEtDistance(Path chemin, double distance) {
            this.chemin = chemin;
            this.distance = distance;
        }
    }
}