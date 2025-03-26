package tsp.autres.hillClimbing.fourmis;
import tsp.evaluation.Coordinates;
import tsp.evaluation.Evaluation;
import tsp.evaluation.Path;
import tsp.projects.CompetitorProject;
import tsp.projects.InvalidProjectException;

import java.util.ArrayList;
import java.util.Random;

public class ColonieFourmis extends CompetitorProject {
    private static final int NB_FOURMIS = 50;
    private static final int NB_ITERATIONS = 200;
    private static final double TAUX_EVAPORATION = 0.5;
    private static final double ALPHA = 1.0; // Influence des phéromones
    private static final double BETA = 2.0;  // Influence de la visibilité (1/distance)
    private static final double Q = 100.0;   // Constante pour le dépôt de phéromones
    private static final double PHEROMONE_INITIAL = 0.1;

    private Random random;
    private Evaluation evaluation;
    private double[][] pheromones;
    private Path meilleurChemin;
    private double meilleureDistance;

    public ColonieFourmis(Evaluation evaluation) throws InvalidProjectException {
        super(evaluation);
        this.addAuthor("Mohamed Krouchi");
        this.addAuthor("Emma Houver");
        this.setMethodName("Colonie de fourmis");
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
        int nbVilles = this.problem.getLength();

        for (int iteration = 0; iteration < NB_ITERATIONS; iteration++) {
            ArrayList<Path> cheminsFourmis = new ArrayList<>();
            ArrayList<Double> distancesFourmis = new ArrayList<>();

            // Chaque fourmi construit son chemin
            for (int fourmi = 0; fourmi < NB_FOURMIS; fourmi++) {
                CheminEtDistance resultat = construireChemin(nbVilles);
                cheminsFourmis.add(resultat.chemin);
                distancesFourmis.add(resultat.distance);

                // Mise à jour du meilleur chemin trouvé
                if (resultat.distance < this.meilleureDistance) {
                    this.meilleureDistance = resultat.distance;
                    this.meilleurChemin = resultat.chemin;
                    this.evaluation.evaluate(this.meilleurChemin);
                }
            }

            // Évaporation des phéromones
            evaporerPheromones(nbVilles);

            // Dépôt de phéromones
            deposerPheromones(cheminsFourmis, distancesFourmis);
        }

        this.evaluation.evaluate(this.meilleurChemin);
    }

    private CheminEtDistance construireChemin(int nbVilles) {
        int[] chemin = new int[nbVilles];
        boolean[] visite = new boolean[nbVilles];

        // Ville de départ aléatoire
        int villeCourante = random.nextInt(nbVilles);
        chemin[0] = villeCourante;
        visite[villeCourante] = true;

        // Construction du chemin
        for (int etape = 1; etape < nbVilles; etape++) {
            int villeSuivante = choisirVilleSuivante(villeCourante, visite);
            chemin[etape] = villeSuivante;
            visite[villeSuivante] = true;
            villeCourante = villeSuivante;
        }

        Path path = new Path(chemin);
        double distance = this.evaluation.quickEvaluate(path);
        return new CheminEtDistance(path, distance);
    }

    private int choisirVilleSuivante(int villeCourante, boolean[] visite) {
        double[] probabilites = new double[visite.length];
        double somme = 0.0;

        // Calcul des probabilités
        for (int ville = 0; ville < visite.length; ville++) {
            if (!visite[ville]) {
                double pheromone = Math.pow(pheromones[villeCourante][ville], ALPHA);
                double visibilite = Math.pow(1.0 / calculerDistance(villeCourante, ville), BETA);
                probabilites[ville] = pheromone * visibilite;
                somme += probabilites[ville];
            }
        }

        // Choix aléatoire pondéré
        double valeurAleatoire = random.nextDouble() * somme;
        double sommePartielle = 0.0;

        for (int ville = 0; ville < visite.length; ville++) {
            if (!visite[ville]) {
                sommePartielle += probabilites[ville];
                if (sommePartielle >= valeurAleatoire) {
                    return ville;
                }
            }
        }

        // Cas par défaut (ne devrait normalement pas se produire)
        for (int ville = 0; ville < visite.length; ville++) {
            if (!visite[ville]) {
                return ville;
            }
        }
        return -1;
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
                int villeA = villes[i];
                int villeB = villes[i + 1];
                pheromones[villeA][villeB] += quantitePheromones;
                pheromones[villeB][villeA] += quantitePheromones;
            }

            // Boucler entre la dernière ville et la première
            int derniereVille = villes[villes.length - 1];
            int premiereVille = villes[0];
            pheromones[derniereVille][premiereVille] += quantitePheromones;
            pheromones[premiereVille][derniereVille] += quantitePheromones;
        }
    }

    private double calculerDistance(int ville1, int ville2) {
        Coordinates c1 = this.problem.getCoordinates(ville1);
        Coordinates c2 = this.problem.getCoordinates(ville2);
        return c1.distance(c2);
    }

    // Classe interne pour stocker un chemin et sa distance
    private class CheminEtDistance {
        Path chemin;
        double distance;

        public CheminEtDistance(Path chemin, double distance) {
            this.chemin = chemin;
            this.distance = distance;
        }
    }
}