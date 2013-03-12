
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 *
 * @author Clark
 */
public class juggle_fest {

    /**
     * Entry point of execution
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        File f = null;
        if (args.length == 0) {
            f = new File("jugglefest.txt");
        } else if (args.length == 1) {
            f = new File(args[0]);
        } else if (args.length > 1) {
            System.err.println("Input syntax: java -jar juggle_fest.jar");
            System.err.println("OR");
            System.err.println("java -jar juggle_fest <fileName>");
            System.exit(0);
        }

        ArrayList<Circuit> circuits = new ArrayList<>();
        ArrayList<Juggler> jugglers = new ArrayList<>();

        //Parse an input file and populate the lists
        JuggleFileIO.populateLists(f, circuits, jugglers);

        // Match each juggler to a circuit

        StableJugglefestMatcher matcher = new StableJugglefestMatcher(circuits, jugglers);
        //System.out.println("Matching jugglers to circuits.");
        matcher.match();

        JuggleFileIO.writeToFile("output.txt", matcher);

        if (args.length == 0 || f.getName().equals("jugglefest.txt")) {
            System.out.println("The sum of the IDs of the jugglers assigned to circuit 1970 is " + matcher.getJugglerSum(1970));
        }
        //System.out.println(matcher.getJugglerSum(1970));
    }
}

/**
 * Class with two static methods used to parse and output juggle_fest files
 *
 * @author Clark
 */
class JuggleFileIO {

    /**
     * Parses a file containing juggler and circuit information
     *
     * @param f
     * @param circuits
     * @param jugglers
     * @throws FileNotFoundException
     * @throws IOException
     */
    static void populateLists(File f, ArrayList<Circuit> circuits, ArrayList<Juggler> jugglers) throws FileNotFoundException, IOException {
        BufferedReader in;
        in = new BufferedReader(new FileReader(f));
        Circuit circuit;

        try {
            while (in.ready()) {
                String[] tokens = in.readLine().split("\\s");
                switch (tokens[0]) {
                    case "J": {
                        int jugglerID = Integer.valueOf(tokens[1].substring(1));
                        int handEye = Integer.valueOf(tokens[2].substring(2));
                        int endurance = Integer.valueOf(tokens[3].substring(2));
                        int pizzazz = Integer.valueOf(tokens[4].substring(2));
                        String[] prefTokens = tokens[5].split(",");
                        int[] preferences = new int[prefTokens.length];
                        int[] scores = new int[preferences.length];
                        for (int prefIndex = 0; prefIndex < preferences.length; prefIndex++) {
                            preferences[prefIndex] = Integer.valueOf(prefTokens[prefIndex].substring(1));
                            circuit = circuits.get(preferences[prefIndex]);
                            scores[prefIndex] = handEye * circuit.getHandEye() + endurance * circuit.getEndurance() + pizzazz * circuit.getPizzazz(); //dot-product
                        }
                        jugglers.add(new Juggler(jugglerID, handEye, endurance, pizzazz, preferences, scores));
                        break;
                    }
                    case "C": {
                        int circuitID = Integer.valueOf(tokens[1].substring(1));
                        int handEye = Integer.valueOf(tokens[2].substring(2));
                        int endurance = Integer.valueOf(tokens[3].substring(2));
                        int pizzazz = Integer.valueOf(tokens[4].substring(2));
                        circuits.add(new Circuit(circuitID, handEye, endurance, pizzazz));
                        break;
                    }
                }


            }
        } finally {
            in.close();
        }
    }

    /**
     * Writes the matcher's results to a text file
     *
     * @param fileName
     * @param matcher
     */
    static public void writeToFile(String fileName, StableJugglefestMatcher matcher) throws FileNotFoundException, IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName))) {
            writer.write(matcher.toString());
        }
    }
}

/**
 * Inspired by the Stable Matching Algorithm as described in
 *
 * Algorithm Design by Jon Kleinberg and Eva Tardos p. 1
 *
 * @author Clark
 */
class StableJugglefestMatcher {

    private int circuitsSize;							// Number of Circuits to be matched
    private int jugglersSize;							// Number of Jugglers to be matched
    private int jugglersPerCircuit;							// Number of Jugglers per Circuit
    private ArrayList<Circuit> myCircuits;
    //private ArrayList<Juggler> myJugglers;
    private ArrayList<Juggler> unassignedJugglers;

    /**
     * Constructor
     */
    public StableJugglefestMatcher(ArrayList<Circuit> circuits, ArrayList<Juggler> jugglers) {
        myCircuits = circuits;
        //myJugglers = jugglers;
        unassignedJugglers = jugglers;
        //unassignedJugglers = new ArrayList<>(myJugglers); //shallow copy
        circuitsSize = circuits.size();
        jugglersSize = jugglers.size();
        jugglersPerCircuit = jugglersSize / circuitsSize;
    }

    public static int computeDotProduct(Circuit circuit, Juggler juggler) {
        return juggler.getHandEye() * circuit.getHandEye()
                + juggler.getEndurance() * circuit.getEndurance()
                + juggler.getPizzazz() * circuit.getPizzazz();
    }

    /**
     * Matches jugglers to each circuit. Uses stable matching algorithm as described in Algorithm Design text
     */
    public void match() {

        //boolean allJugglersAssigned = false;
        //creates a new random generator using current system time as a seed

        Random randomGenerator = new Random(System.currentTimeMillis());
        //scratch variable declaration
        Juggler currentJuggler;
        Circuit currentCircuit;
        int preferenceIndex;
        int[] preferences, scores;
        int preferencesLength; //for optimization
        boolean currMatched;

        //iterate over all jugglers & match each juggler to a circuit
        //keep doing this loop until all jugglers have been assigned

        while (!unassignedJugglers.isEmpty()) {
            for (int unassignedIndex = 0; unassignedIndex < unassignedJugglers.size(); unassignedIndex++) { //cannot optimize because unassignedJugglers.size() changes after every modification
                currentJuggler = unassignedJugglers.get(unassignedIndex);
                currMatched = false;
                preferenceIndex = currentJuggler.getIndex() + 1;
                preferences = currentJuggler.getPreferences();
                preferencesLength = preferences.length;
                scores = currentJuggler.getScores();
                
                //iterate through all circuits in a juggler's preference list
                while (preferenceIndex < preferencesLength) {
                    currentCircuit = myCircuits.get(preferences[preferenceIndex]);
                    currentJuggler.setCurrentCircuit(currentCircuit.getID()); //associate the juggler with the current circuit
                    currentJuggler.setCurrentScore(scores[preferenceIndex]);//give the juggler a score
                    currentJuggler.setIndex(preferenceIndex);

                    //if there is already space for the juggler, simply add it to the circuit
                    if (currentCircuit.getJugglers().size() < jugglersPerCircuit) {
                        currentJuggler.setMatched(true);
                        currentCircuit.addJuggler(currentJuggler);
                        unassignedJugglers.remove(unassignedIndex);
                        currMatched = true;
                        break;

                    } //else if the juggler has a higher score than the circuit's minimum score, replace the lowest scoring juggler with this one
                    else if (currentJuggler.getCurrentScore() > currentCircuit.getMinScore()) {
                        unassignedJugglers.add(currentCircuit.replaceWorstJuggler(currentJuggler));
                        unassignedJugglers.remove(unassignedIndex);
                        currMatched = true;
                        break;
                    } else //if neither is possible, go to the next circuit on the juggler's preference list
                    {
                        preferenceIndex++;
                    }
                }
                //if the juggler is still unassigned, assign it to a random circuit
                if (!currMatched) {
                    while (true) {
                        Circuit randomCircuit = myCircuits.get(randomGenerator.nextInt(circuitsSize));
                        currentJuggler.setCurrentCircuit(randomCircuit.getID());
                        currentJuggler.setCurrentScore(StableJugglefestMatcher.computeDotProduct(randomCircuit, unassignedJugglers.get(unassignedIndex)));

                        //if the circuit is not filled, assign this juggler to it
                        if (randomCircuit.getJugglers().size() < jugglersPerCircuit) {
                            currentJuggler.setMatched(true);
                            randomCircuit.addJuggler(currentJuggler);
                            unassignedJugglers.remove(unassignedIndex);
                            break;

                            //else if this juggler has a higher score than the circuit's minimum score, replace that circuit's worst juggler
                        } else if (currentJuggler.getCurrentScore() > randomCircuit.getMinScore()) {
                            unassignedJugglers.add(randomCircuit.replaceWorstJuggler(currentJuggler));
                            unassignedJugglers.remove(unassignedIndex);
                            break;
                        }
                    }
                }
            }
        }
	}

    /**
     * Used to find the output as per Yodle's specifications
     *
     * @param circuitID the ID of the circuit whose juggler IDs are to be summed
     * @return the sum of each juggler ID assigned to the circuit
     */
    public int getJugglerSum(int circuitID) {
        int sum = 0;
        Circuit circuit = myCircuits.get(circuitID);
        for (Juggler j : circuit.getJugglers()) {
            sum += j.getID();
        }
        return sum;
    }

    /**
     * String representation of the circuits and jugglers
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator");
        //String lineSeparator = System.getProperty("\n");
        for (int index = circuitsSize - 1; index >= 0; index--) {
            Circuit c = myCircuits.get(index);
            builder.append(c).append(lineSeparator);
        }
        return builder.toString();
    }
}

/**
 * Representation of a juggler
 *
 * @author Clark
 */
class Juggler implements Comparable {

    private boolean isMatched;
    private int myID;
    private int myHandEye;
    private int myEndurance;
    private int myPizzazz;
    private int[] myPreferences;
    private int[] myScores;
    private int myCurrentCircuit;
    private int myCurrentScore;
    private int myIndex;
    private static final int NONE = -1;

    /**
     * Constructor
     */
    public Juggler(int id, int handEye, int endurance, int pizzazz, int[] preferences, int[] scores) {
        myID = id;
        myHandEye = handEye;
        myEndurance = endurance;
        myPizzazz = pizzazz;
        myPreferences = preferences;
        myScores = scores;
        isMatched = false;
        myCurrentCircuit = NONE;
        myCurrentScore = NONE;
        myIndex = NONE;
    }

    /**
     * Accessor
     *
     * @return whether or not the Juggler is currently matched
     */
    public boolean isMatched() {
        return isMatched;
    }

    /**
     * Accessor method
     *
     * @return the ID number of this Juggler (they start at 0 and are
     * consecutive)
     */
    public int getNumber() {
        return myID;
    }

    /**
     * Accessor method
     *
     * @return this Juggler's endurance rating
     */
    public int getEndurance() {
        return myEndurance;
    }

    /**
     * Accessor method
     *
     * @return this Juggler's hand-eye coordination rating
     */
    public int getHandEye() {
        return myHandEye;
    }

    /**
     * Accessor method
     *
     * @return this Juggler's pizzazz rating
     */
    public int getPizzazz() {
        return myPizzazz;
    }

    /**
     * Accessor
     *
     * @return the corresponding scores for each circuit listed in
     * getPreferences()
     */
    public int[] getScores() {
        return myScores;
    }

    /**
     * Accessor
     *
     * @return an array of ints representing this juggler's preferences, with
     * each int corresponding to a circuit
     */
    public int[] getPreferences() {
        return myPreferences;
    }

    /**
     * Accessor
     *
     * @return the Circuit this Juggler is currently assigned to
     */
    public int getCurrentCircuit() {
        return myCurrentCircuit;
    }

    /**
     * Accessor
     *
     * @return the score that this Juggler achieves when matched with its
     * current circuit
     */
    public int getCurrentScore() {
        return myCurrentScore;
    }

    /**
     * The index of the this Juggler's currently assigned circuit in
     * getPreferences()
     *
     * @return
     */
    public int getIndex() {
        return myIndex;
    }

    /**
     * Accessor
     *
     * @return
     */
    public int getID() {
        return myID;
    }

    /**
     * Mutator
     *
     * @param b
     */
    public void setMatched(boolean b) {
        isMatched = b;
    }

    /**
     * Mutator
     *
     * @param circuitID the ID of the new current circuit
     */
    public void setCurrentCircuit(int circuitID) {
        myCurrentCircuit = circuitID;
    }

    /**
     * Mutator
     *
     * @param score - the new score
     */
    public void setCurrentScore(int score) {
        myCurrentScore = score;
    }

    /**
     * Mutator
     *
     * @param index
     */
    void setIndex(int index) {
        myIndex = index;
    }

    @Override
    /**
     * Explicit implementation of Comparable interface so Jugglers may be sorted
     * using a Comparator
     */
    public int compareTo(Object o) {
        Juggler j = (Juggler) o;
        if (myCurrentScore < j.getCurrentScore()) {
            return 1;
        } else if (myCurrentScore == j.getCurrentScore()) {
            return 0;
        } else {
            return -1;
        }
    }

    /**
     *
     * @return string representation of a juggler
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("J").append(myID);

        for (int i = 0; i < myPreferences.length; ++i) {
            sb.append(" C").append(myPreferences[i]).append(":").append(myScores[i]);
        }

        return sb.toString();
    }
}

/**
 * Representation of a circuit
 *
 * @author Clark
 */
class Circuit {

    private int myID;
    private int myHandEye;
    private int myEndurance;
    private int myPizzazz;
    private ArrayList<Juggler> myJugglers;
    //private int jugglersSize;
    private int myMinScore;

    /**
     * Constructor
     */
    public Circuit(int id, int handEye, int endurance, int pizzazz) {
        myID = id;
        myHandEye = handEye;
        myEndurance = endurance;
        myPizzazz = pizzazz;
        myJugglers = new ArrayList<>();
        myMinScore = Integer.MAX_VALUE;
        //jugglersSize = myJugglers.size(); //for optimization
    }

    /**
     * Accessor
     *
     * @return the ID of this circuit (they start at 1 and run consecutively)
     */
    public int getID() {
        return myID;
    }

    /**
     * Accessor
     *
     * @return the rating of the hand-eye coordination aspect of this circuit
     */
    public int getHandEye() {
        return myHandEye;
    }

    /**
     * Accessor
     *
     * @return the rating of the endurance aspect of this circuit
     */
    public int getEndurance() {
        return myEndurance;
    }

    /**
     * Accessor
     *
     * @return the rating of the pizzazz aspect of this circuit
     */
    public int getPizzazz() {
        return myPizzazz;
    }

    /**
     * Accessor
     *
     * @return the lowest score achieved by all jugglers assigned to this
     * circuit
     */
    public int getMinScore() {
        return myMinScore;
    }

    /**
     * Accessor
     *
     * @return the list of jugglers currently assigned to this circuit
     *
     */
    public ArrayList<Juggler> getJugglers() {
        return myJugglers;
    }

    /**
     * Adds a new juggler and sorts
     *
     * @param newJuggler - the new juggler to be added
     */
    public void addJuggler(Juggler newJuggler) {
        myJugglers.add(newJuggler);
        sortJugglers();
        myMinScore = myJugglers.get(myJugglers.size() - 1).getCurrentScore();
    }

    /**
     * Replaces the worst juggler for this circuit with a new juggler
     *
     * @param newJuggler - the new Juggler to be added to the circuit
     * @return - the worst juggler that was removed from this circuit
     */
    public Juggler replaceWorstJuggler(Juggler newJuggler) {

        Juggler removedJuggler = myJugglers.remove(myJugglers.size() - 1);
        removedJuggler.setMatched(false);
        newJuggler.setMatched(true);
        addJuggler(newJuggler);

        return removedJuggler;
    }

    /**
     * Sorts the Jugglers by comparing their current scores
     */
    private void sortJugglers() {

        /**
         * From the API:
         *
         * This implementation is a stable, adaptive, iterative mergesort that
         * requires far fewer than n lg(n) comparisons when the input array is
         * partially sorted, while offering the performance of a traditional
         * mergesort when the input array is randomly ordered. If the input
         * array is nearly sorted, the implementation requires approximately n
         * comparisons.
         */
        Collections.sort(myJugglers);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("C").append(myID).append(" ");

        for (int i = 0; i < myJugglers.size() - 1; i++) {
            builder.append(myJugglers.get(i)).append(",");
        }
        builder.append(myJugglers.get(myJugglers.size() - 1));

        return builder.toString();
    }
}
