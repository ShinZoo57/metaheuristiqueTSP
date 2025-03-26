package tsp.autres.hillClimbing;

import tsp.evaluation.Evaluation;
import tsp.evaluation.Path;
import tsp.projects.CompetitorProject;
import tsp.projects.InvalidProjectException;

import java.util.ArrayList;
import java.util.Random;

public class hillClimbing extends CompetitorProject {
    private Path chemin;
    private double cout;
    private Random random;
    public hillClimbing(Evaluation evaluation) throws InvalidProjectException {
        super(evaluation);
        //this.addAuthor ("Mohamed Krouchi");
        //this.setMethodName ("hillClimbing");
    }

    @Override
    public void initialization() {

        int length = this.problem.getLength();
        int [] path  = Path.getRandomPath(length);
        Path chemin = new Path(path);
        cout = evaluation.evaluate(chemin);
        random = new Random();
    }

    @Override
    public void loop() {
        ArrayList<Integer> bestNeighbor = null;
        double bestNeighborCost = cout;
/*
        // Génération des voisins (interversion de deux villes)
        for (int i = 0; i < chemin.getPath().length - 1; i++) {
            for (int j = i + 1; j < solution.size(); j++) {
                ArrayList<Integer> neighbor = new ArrayList<>(solution);
                Collections.swap(neighbor, i, j);
                double neighborCost = evaluation.evaluate(neighbor);

                // Sélection du meilleur voisin
                if (neighborCost < bestNeighborCost) {
                    bestNeighbor = neighbor;
                    bestNeighborCost = neighborCost;
                }
            }
        }

        // Si on trouve une meilleure solution, on l'accepte
        if (bestNeighbor != null) {
            solution = bestNeighbor;
            cout = bestNeighborCost;
        } else {
            // Si aucun voisin n'améliore la solution, on stoppe l'algorithme (optimum local atteint)
            Thread.currentThread().interrupt();
        }

 */
    }
}
