package tsp.projects.fourmisGrasp;

import tsp.evaluation.Coordinates;
import tsp.evaluation.Evaluation;
import tsp.evaluation.Path;
import tsp.projects.CompetitorProject;
import tsp.projects.InvalidProjectException;

import java.util.ArrayList;
import java.util.Random;

public class ColonieFourmisGRASP extends CompetitorProject {
    private static final int NB_FOURMIS = 130;
    private static final int NB_ITERATIONS = 200;
    private static final double TAUX_EVAPORATION = 0.5;
    private static final double Q = 100.0;
    private static final double PHEROMONE_INITIAL = 0.1;

    private Random random;
    private Evaluation evaluation;
    private double[][] pheromones;
    private Path meilleurChemin;
    private double meilleureDistance;

    public ColonieFourmisGRASP(Evaluation evaluation) throws InvalidProjectException {
        super(evaluation);
        this.addAuthor("Mohamed Krouchi");
        this.addAuthor("Emma Houver");
        this.setMethodName("Colonie de fourmis + GRASP");
        this.evaluation = evaluation;
        this.random = new Random();
    }

    @Override
    public void initialization() {
        int nbVilles = this.problem.getLength();
        initialiserPheromones(nbVilles);
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

    @Override
    public void loop() {
        long startTime = System.currentTimeMillis();
        long timeLimit = 70 * 1000; // 60 secondes en millisecondes

        int nbVilles = this.problem.getLength();

        for (int iteration = 0; iteration < NB_ITERATIONS; iteration++) {
            if (System.currentTimeMillis() - startTime >= timeLimit) {
                System.out.println("Temps écoulé : arrêt de l'algorithme.");
                break;
            }

            ArrayList<Path> cheminsFourmis = new ArrayList<>();
            ArrayList<Double> distancesFourmis = new ArrayList<>();

            for (int fourmi = 0; fourmi < NB_FOURMIS; fourmi++) {
                CheminEtDistance resultat = construireCheminGRASP(nbVilles);
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


    private CheminEtDistance construireCheminGRASP(int nbVilles) {
        int[] chemin = new int[nbVilles];
        boolean[] visite = new boolean[nbVilles];

        int villeCourante = random.nextInt(nbVilles);
        chemin[0] = villeCourante;
        visite[villeCourante] = true;

        for (int etape = 1; etape < nbVilles; etape++) {
            int villeSuivante = choisirVilleSuivanteGRASP(villeCourante, visite);
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

    private int choisirVilleSuivanteGRASP(int villeCourante, boolean[] visite) {
        ArrayList<Integer> candidats = new ArrayList<>();
        double minDistance = Double.MAX_VALUE, maxDistance = Double.MIN_VALUE;

        for (int ville = 0; ville < visite.length; ville++) {
            if (!visite[ville]) {
                double distance = calculerDistance(villeCourante, ville);
                if (distance < minDistance) minDistance = distance;
                if (distance > maxDistance) maxDistance = distance;
                candidats.add(ville);
            }
        }

        double seuil = minDistance + 0.2 * (maxDistance - minDistance);
        ArrayList<Integer> RCL = new ArrayList<>();

        for (int ville : candidats) {
            if (calculerDistance(villeCourante, ville) <= seuil) {
                RCL.add(ville);
            }
        }

        return RCL.get(random.nextInt(RCL.size()));
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

        double ancienneDistance = calculerDistance(villeA, villeB) + calculerDistance(villeC, villeD);
        double nouvelleDistance = calculerDistance(villeA, villeC) + calculerDistance(villeB, villeD);

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
