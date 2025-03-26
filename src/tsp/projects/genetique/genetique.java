package tsp.projects.genetique;

import tsp.evaluation.Coordinates;
import tsp.evaluation.Evaluation;
import tsp.evaluation.Path;
import tsp.projects.CompetitorProject;
import tsp.projects.InvalidProjectException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class genetique extends CompetitorProject {

    private static final int NB_INDIVIDUS = 50;
    private static final int NB_GENERATION = 100;
    private static final double MUTATION_RATE = 0.2;
    private Random random;
    private ArrayList<Path> population;
    private Path bestPath;

    public genetique(Evaluation evaluation) throws InvalidProjectException {
        super(evaluation);
        this.addAuthor("Mohamed Krouchi");
        this.setMethodName("génétique amélioré");
    }

    @Override
    public void initialization() {
        int length = this.problem.getLength();
        population = new ArrayList<>();
        random = new Random();

        // Génération de la population initiale avec différentes méthodes
        for (int i = 0; i < NB_INDIVIDUS; i++) {
            Path chemin;
            if (i < NB_INDIVIDUS * 0.7) {
                chemin = genererCheminPlusProcheV2(length); // 70% avec méthode améliorée
            } else if (i < NB_INDIVIDUS * 0.9) {
                chemin = genererCheminAleatoire(length); // 20% aléatoires
            } else {
                chemin = genererCheminPlusProcheDepuisCentre(length); // 10% depuis le centre
            }
            population.add(chemin);
        }
    }

    private Path genererCheminPlusProcheV2(int length) {
        int[] path = new int[length];
        boolean[] visited = new boolean[length];
        int current = random.nextInt(length);
        path[0] = current;
        visited[current] = true;

        for (int i = 1; i < length; i++) {
            // Trouver les 5 plus proches voisins non visités
            ArrayList<Integer> meilleursVoisins = new ArrayList<>();
            ArrayList<Double> distances = new ArrayList<>();

            for (int j = 0; j < length; j++) {
                if (!visited[j]) {
                    double distance = calculerDistance(current, j);
                    if (meilleursVoisins.size() < 5) {
                        meilleursVoisins.add(j);
                        distances.add(distance);
                    } else {
                        double maxDistance = Collections.max(distances);
                        if (distance < maxDistance) {
                            int index = distances.indexOf(maxDistance);
                            meilleursVoisins.set(index, j);
                            distances.set(index, distance);
                        }
                    }
                }
            }

            // Choisir aléatoirement parmi les 5 meilleurs
            current = meilleursVoisins.get(random.nextInt(meilleursVoisins.size()));
            path[i] = current;
            visited[current] = true;
        }

        return new Path(path);
    }

    private Path genererCheminAleatoire(int length) {
        ArrayList<Integer> villes = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            villes.add(i);
        }
        Collections.shuffle(villes);
        int[] path = new int[length];
        for (int i = 0; i < length; i++) {
            path[i] = villes.get(i);
        }
        return new Path(path);
    }

    private Path genererCheminPlusProcheDepuisCentre(int length) {
        int[] path = new int[length];
        boolean[] visited = new boolean[length];

        // Trouver la ville la plus centrale
        int villeCentrale = trouverVilleCentrale(length);
        path[0] = villeCentrale;
        visited[villeCentrale] = true;

        // Utiliser l'approche du plus proche voisin depuis le centre
        int current = villeCentrale;
        for (int i = 1; i < length; i++) {
            int meilleurVoisin = -1;
            double distanceMin = Double.MAX_VALUE;

            for (int j = 0; j < length; j++) {
                if (!visited[j]) {
                    double distance = calculerDistance(current, j);
                    if (distance < distanceMin) {
                        distanceMin = distance;
                        meilleurVoisin = j;
                    }
                }
            }

            path[i] = meilleurVoisin;
            visited[meilleurVoisin] = true;
            current = meilleurVoisin;
        }

        return new Path(path);
    }

    private int trouverVilleCentrale(int length) {
        Coordinates centre = calculerCentreGeometrique(length);
        int villeCentrale = 0;
        double distanceMin = Double.MAX_VALUE;

        for (int i = 0; i < length; i++) {
            Coordinates c = this.problem.getCoordinates(i);
            double distance = c.distance(centre);
            if (distance < distanceMin) {
                distanceMin = distance;
                villeCentrale = i;
            }
        }
        return villeCentrale;
    }

    private Coordinates calculerCentreGeometrique(int length) {
        double x = 0, y = 0;
        for (int i = 0; i < length; i++) {
            Coordinates c = this.problem.getCoordinates(i);
            x += c.getX();
            y += c.getY();
        }
        return new Coordinates(x / length, y / length);
    }

    private double calculerDistance(int ville1, int ville2) {
        Coordinates c1 = this.problem.getCoordinates(ville1);
        Coordinates c2 = this.problem.getCoordinates(ville2);
        return c1.distance(c2);
    }

    private Path mutation(Path path) {
        int[] newPath = path.getCopyPath();
        double mutationChoice = random.nextDouble();

        if (mutationChoice < 0.4) {
            // Swap mutation
            int i = random.nextInt(newPath.length);
            int j = random.nextInt(newPath.length);
            int temp = newPath[i];
            newPath[i] = newPath[j];
            newPath[j] = temp;
        } else if (mutationChoice < 0.8) {
            // Inversion mutation
            int i = random.nextInt(newPath.length);
            int j = random.nextInt(newPath.length);
            if (i > j) {
                int temp = i;
                i = j;
                j = temp;
            }
            while (i < j) {
                int temp = newPath[i];
                newPath[i] = newPath[j];
                newPath[j] = temp;
                i++;
                j--;
            }
        } else {
            // Déplacement mutation
            int i = random.nextInt(newPath.length);
            int j = random.nextInt(newPath.length);
            int ville = newPath[i];
            if (i < j) {
                System.arraycopy(newPath, i+1, newPath, i, j-i);
            } else {
                System.arraycopy(newPath, j, newPath, j+1, i-j);
            }
            newPath[j] = ville;
        }

        return new Path(newPath);
    }

    private Path croisement(Path parent1, Path parent2) {
        int length = parent1.getPath().length;
        int[] enfant = new int[length];
        boolean[] visited = new boolean[length];

        // Ordered Crossover (OX)
        int start = random.nextInt(length);
        int end = random.nextInt(length - start) + start;

        // Copier la séquence du parent1
        for (int i = start; i <= end; i++) {
            enfant[i] = parent1.getPath()[i];
            visited[enfant[i]] = true;
        }

        // Remplir avec les villes du parent2 dans l'ordre
        int position = (end + 1) % length;
        for (int i = 0; i < length; i++) {
            int index = (end + 1 + i) % length;
            int ville = parent2.getPath()[index];
            if (!visited[ville]) {
                enfant[position] = ville;
                visited[ville] = true;
                position = (position + 1) % length;
            }
        }

        return new Path(enfant);
    }

    private double fitness(Path path) {
        return this.evaluation.evaluate(path);
    }

    private Path selection() {
        // Sélection par tournoi avec taille dynamique
        int tournoiSize = Math.max(2, (int)(population.size() * 0.1));
        Path best = population.get(random.nextInt(population.size()));

        for (int i = 1; i < tournoiSize; i++) {
            Path challenger = population.get(random.nextInt(population.size()));
            if (fitness(challenger) < fitness(best)) {
                best = challenger;
            }
        }
        return best;
    }

    @Override
    public void loop() {
        for (int generation = 0; generation < NB_GENERATION; generation++) {
            ArrayList<Path> newPopulation = new ArrayList<>();

            // Trier la population par fitness
            population.sort((p1, p2) -> Double.compare(fitness(p1), fitness(p2)));

            // Élitisme - garder les 10% meilleurs
            int eliteSize = Math.max(1, (int)(NB_INDIVIDUS * 0.1));
            for (int i = 0; i < eliteSize; i++) {
                newPopulation.add(population.get(i));
            }

            // Remplir le reste de la population
            while (newPopulation.size() < NB_INDIVIDUS) {
                Path parent1 = selection();
                Path parent2 = selection();

                // 10% de chance de faire une mutation sans croisement
                if (random.nextDouble() < 0.1) {
                    Path mutant = mutation(parent1);
                    newPopulation.add(mutant);
                } else {
                    // Croisement seulement si les parents sont différents
                    Path enfant = parent1.equals(parent2) ? parent1 : croisement(parent1, parent2);

                    // Mutation adaptative
                    double adaptiveRate = MUTATION_RATE * (1 - (double)generation/NB_GENERATION);
                    if (random.nextDouble() < adaptiveRate) {
                        enfant = mutation(enfant);
                    }

                    newPopulation.add(enfant);
                }
            }

            population = newPopulation;

            // Mettre à jour le meilleur chemin global
            Path generationBest = Collections.min(population, (p1, p2) -> Double.compare(fitness(p1), fitness(p2)));
            if (bestPath == null || fitness(generationBest) < fitness(bestPath)) {
                bestPath = generationBest;
            }
        }

        this.evaluation.evaluate(bestPath);
    }
}