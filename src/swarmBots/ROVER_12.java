package swarmBots;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import common.Coord;
import common.MapTile;
import common.ScanMap;
import enums.Terrain;

/**
 * The seed that this program is built on is a chat program example found here:
 * http://cs.lmu.edu/~ray/notes/javanetexamples/ Many thanks to the authors for
 * publishing their code examples
 * 
 * ROVER_12 Spec: Drive = wheels, Tool 1 = spectral sensor, Tool 2 = range
 * extender
 */

// this comment is for commit
public class ROVER_12 {

	protected BufferedReader in;
	protected PrintWriter out;
	protected String rovername, line;
	protected ScanMap scanMap;
	protected Coord targetLoc, startLoc, currrentLoc,previousLoc;
	protected int sleepTime;
	protected String SERVER_ADDRESS = "localhost";
	protected static final int PORT_ADDRESS = 9537;

	public ROVER_12() {
		// constructor
		System.out.println("ROVER_12 rover object constructed");
		rovername = "ROVER_12";
		SERVER_ADDRESS = "localhost";
		// this should be a safe but slow timer value
		sleepTime = 300; // in milliseconds - smaller is faster, but the server
							// will cut connection if it is too small
	}

	public ROVER_12(String serverAddress) {
		// constructor
		System.out.println("ROVER_12 rover object constructed");
		rovername = "ROVER_12";
		SERVER_ADDRESS = serverAddress;
		sleepTime = 200; // in milliseconds - smaller is faster, but the server
							// will cut connection if it is too small
	}

	/**
	 * Connects to the server then enters the processing loop.
	 */
	private void run() throws IOException, InterruptedException {

		// Make connection and initialize streams
		// TODO - need to close this socket
		Socket socket = new Socket(SERVER_ADDRESS, PORT_ADDRESS); // set port
																	// here
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);

		// Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// Process all messages from server, wait until server requests Rover ID
		// name
		while (true) {
			String line = in.readLine();
			if (line.startsWith("SUBMITNAME")) {
				out.println(rovername); // This sets the name of this instance
										// of a swarmBot for identifying the
										// thread to the server
				break;
			}
		}

		// ******** Rover logic *********
		// int cnt=0;
		String line = "";

		boolean goingSouth = false;
		boolean stuck = false; // just means it did not change locations between
								// requests,
								// could be velocity limit or obstruction etc.
		boolean blocked = false;

		String[] cardinals = new String[4];
		cardinals[0] = "N";
		cardinals[1] = "E";
		cardinals[2] = "S";
		cardinals[3] = "W";

		String currentDir = cardinals[0];

		// start Rover controller process
		while (true) {

			// currently the requirements allow sensor calls to be made with no
			// simulated resource cost

			// **** location call ****
			out.println("LOC");
			line = in.readLine();
			if (line == null) {
				System.out.println("ROVER_12 check connection to server");
				line = "";
			}
			if (line.startsWith("LOC")) {
				// loc = line.substring(4);
				currentLoc = extractCurrLOC(line);
			}
			System.out.println("ROVER_12 currentLoc at start: " + currentLoc);

			// after getting location set previous equal current to be able to
			// check for stuckness and blocked later
			previousLoc = currentLoc;

			// **** get equipment listing ****
			ArrayList<String> equipment = new ArrayList<String>();
			equipment = getEquipment();
			// System.out.println("ROVER_12 equipment list results drive " +
			// equipment.get(0));
			System.out.println("ROVER_12 equipment list results " + equipment
					+ "\n");

			// ***** do a SCAN *****
			// System.out.println("ROVER_12 sending SCAN request");
			this.doScan();
			scanMap.debugPrintMap();

			// ***** MOVING *****
			// try moving east 5 block if blocked
			if (blocked) {
				for (int i = 0; i < 5; i++) {
					out.println("MOVE E");
					// System.out.println("ROVER_12 request move E");
					Thread.sleep(300);
				}
				blocked = false;
				// reverses direction after being blocked
				goingSouth = !goingSouth;
			} else {

				// pull the MapTile array out of the ScanMap object
				MapTile[][] scanMapTiles = scanMap.getScanMap();
				int centerIndex = (scanMap.getEdgeSize() - 1) / 2;
				// tile S = y + 1; N = y - 1; E = x + 1; W = x - 1

				if (goingSouth) {
					// check scanMap to see if path is blocked to the south
					// (scanMap may be old data by now)
					if (scanMapTiles[centerIndex][centerIndex + 1]
							.getHasRover()
							|| scanMapTiles[centerIndex][centerIndex + 1]
									.getTerrain() == Terrain.ROCK
							|| scanMapTiles[centerIndex][centerIndex + 1]
									.getTerrain() == Terrain.NONE) {
						blocked = true;
					} else {
						// request to server to move
						out.println("MOVE S");
						// System.out.println("ROVER_12 request move S");
					}

				} else {
					// check scanMap to see if path is blocked to the north
					// (scanMap may be old data by now)
					// System.out.println("ROVER_12 scanMapTiles[2][1].getHasRover() "
					// + scanMapTiles[2][1].getHasRover());
					// System.out.println("ROVER_12 scanMapTiles[2][1].getTerrain() "
					// + scanMapTiles[2][1].getTerrain().toString());

					if (scanMapTiles[centerIndex][centerIndex - 1]
							.getHasRover()
							|| scanMapTiles[centerIndex][centerIndex - 1]
									.getTerrain() == Terrain.ROCK
							|| scanMapTiles[centerIndex][centerIndex - 1]
									.getTerrain() == Terrain.NONE) {
						blocked = true;
					} else {
						// request to server to move
						out.println("MOVE N");
						// System.out.println("ROVER_12 request move N");
					}
				}
			}

			// another call for current location
			out.println("LOC");
			line = in.readLine();
			if (line == null) {
				System.out.println("ROVER_12 check connection to server");
				line = "";
			}
			if (line.startsWith("LOC")) {
				currentLoc = extractCurrLOC(line);
			}

			// System.out.println("ROVER_12 currentLoc after recheck: " +
			// currentLoc);
			// System.out.println("ROVER_12 previousLoc: " + previousLoc);

			// test for stuckness
			stuck = currentLoc.equals(previousLoc);

			// System.out.println("ROVER_12 stuck test " + stuck);
			System.out.println("ROVER_12 blocked test " + blocked);

			// TODO - logic to calculate where to move next

			Thread.sleep(sleepTime);

			System.out
					.println("ROVER_12 ------------ bottom process control --------------");
		}

	}

	// ################ Support Methods ###########################

	private void clearReadLineBuffer() throws IOException {
		while (in.ready()) {
			// System.out.println("ROVER_12 clearing readLine()");
			String garbage = in.readLine();
		}
	}

	// method to retrieve a list of the rover's equipment from the server
	private ArrayList<String> getEquipment() throws IOException {
		// System.out.println("ROVER_12 method getEquipment()");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		out.println("EQUIPMENT");

		String jsonEqListIn = in.readLine(); // grabs the string that was
												// returned first
		if (jsonEqListIn == null) {
			jsonEqListIn = "";
		}
		StringBuilder jsonEqList = new StringBuilder();
		// System.out.println("ROVER_12 incomming EQUIPMENT result - first readline: "
		// + jsonEqListIn);

		if (jsonEqListIn.startsWith("EQUIPMENT")) {
			while (!(jsonEqListIn = in.readLine()).equals("EQUIPMENT_END")) {
				if (jsonEqListIn == null) {
					break;
				}
				// System.out.println("ROVER_12 incomming EQUIPMENT result: " +
				// jsonEqListIn);
				jsonEqList.append(jsonEqListIn);
				jsonEqList.append("\n");
				// System.out.println("ROVER_12 doScan() bottom of while");
			}
		} else {
			// in case the server call gives unexpected results
			clearReadLineBuffer();
			return null; // server response did not start with "EQUIPMENT"
		}

		String jsonEqListString = jsonEqList.toString();
		ArrayList<String> returnList;
		returnList = gson.fromJson(jsonEqListString,
				new TypeToken<ArrayList<String>>() {
				}.getType());
		// System.out.println("ROVER_12 returnList " + returnList);

		return returnList;
	}

	// sends a SCAN request to the server and puts the result in the scanMap
	// array
	public void doScan() throws IOException {
		// System.out.println("ROVER_12 method doScan()");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		out.println("SCAN");

		String jsonScanMapIn = in.readLine(); // grabs the string that was
												// returned first
		if (jsonScanMapIn == null) {
			System.out.println("ROVER_12 check connection to server");
			jsonScanMapIn = "";
		}
		StringBuilder jsonScanMap = new StringBuilder();
		System.out.println("ROVER_12 incomming SCAN result - first readline: "
				+ jsonScanMapIn);

		if (jsonScanMapIn.startsWith("SCAN")) {
			while (!(jsonScanMapIn = in.readLine()).equals("SCAN_END")) {
				// System.out.println("ROVER_12 incomming SCAN result: " +
				// jsonScanMapIn);
				jsonScanMap.append(jsonScanMapIn);
				jsonScanMap.append("\n");
				// System.out.println("ROVER_12 doScan() bottom of while");
			}
		} else {
			// in case the server call gives unexpected results
			clearReadLineBuffer();
			return; // server response did not start with "SCAN"
		}
		// System.out.println("ROVER_12 finished scan while");

		String jsonScanMapString = jsonScanMap.toString();
		// debug print json object to a file
		// new MyWriter( jsonScanMapString, 0); //gives a strange result -
		// prints the \n instead of newline character in the file

		// System.out.println("ROVER_12 convert from json back to ScanMap class");
		// convert from the json string back to a ScanMap object
		scanMap = gson.fromJson(jsonScanMapString, ScanMap.class);
	}

	// this takes the LOC response string, parses out the x and x values and
	// returns a Coord object
	public static Coord extractCurrLOC(String sStr) {
		sStr = sStr.substring(4);
		if (sStr.lastIndexOf(" ") != -1) {
			String xStr = sStr.substring(0, sStr.lastIndexOf(" "));
			// System.out.println("extracted xStr " + xStr);

			String yStr = sStr.substring(sStr.lastIndexOf(" ") + 1);
			// System.out.println("extracted yStr " + yStr);
			return new Coord(Integer.parseInt(xStr), Integer.parseInt(yStr));
		}
		return null;
	}

	// one of the motion dictating method (will be moved and adjusted to the
	// appropriate location)
	public void zigzagMotion(double[][] dct, int block_size, int channel) {

		double[][] temp_dct = new double[block_size][block_size];

		for (int i = 0; i < dct.length; i += 8) {
			for (int j = 0; j < dct[i].length; j += 8) {

				for (int i1 = 0; i1 < dct.length; i1++) {
					for (int j1 = 0; j1 < dct[i1].length; j1++) {
						temp_dct[i1][j1] = dct[i][j];
					}
				}

				// for ( CodeRunLengthPair p : temp_i_rep ) {
				// intermediate_rep.add( p );
				// }
			}
		}
	}

	private Coord requestTargetLoc() throws IOException {

		// setCurrentLoc(currentLoc);

		out.println("TARGET_LOC " + currrentLoc.getXpos() + " " + currrentLoc.getYpos());
		line = in.readLine();

		if (line == null || line == "") {
			// System.out.println("ROVER_12 check connection to server");
			line = "";
		}

		if (line.startsWith("TARGET")) {
			targetLoc = extractTargetLOC(line);
		}
		return targetLoc;
	}

	public static Coord extractTargetLOC(String sStr) {
		sStr = sStr.substring(11);
		if (sStr.lastIndexOf(" ") != -1) {
			String xStr = sStr.substring(0, sStr.lastIndexOf(" "));
			// System.out.println("extracted xStr " + xStr);

			String yStr = sStr.substring(sStr.lastIndexOf(" ") + 1);
			// System.out.println("extracted yStr " + yStr);
			return new Coord(Integer.parseInt(xStr), Integer.parseInt(yStr));
		}
		return null;
	}

	private Coord requestStartLoc() throws IOException {

		// setCurrentLoc(currentLoc);

		out.println("START_LOC " + currrentLoc.getXpos() + " " + currrentLoc.getYpos());
		line = in.readLine();

		if (line == null || line == "") {
			System.out.println("ROVER_12 check connection to server");
			line = "";
		}

		//
		System.out.println();
		if (line.startsWith("START")) {
			startLoc = extractStartLOC(line);
		}
		return startLoc;
	}

	public static Coord extractStartLOC(String sStr) {

		sStr = sStr.substring(10);

		if (sStr.lastIndexOf(" ") != -1) {
			String xStr = sStr.substring(0, sStr.lastIndexOf(" "));
			// System.out.println("extracted xStr " + xStr);

			String yStr = sStr.substring(sStr.lastIndexOf(" ") + 1);
			// System.out.println("extracted yStr " + yStr);
			return new Coord(Integer.parseInt(xStr), Integer.parseInt(yStr));
		}
		return null;
	}

	private Coord setCurrentLoc() throws IOException {
		String line;

		out.println("LOC");
		line = in.readLine();
		if (line == null) {
			// System.out.println("ROVER_12 check connection to server");
			line = "";
		}
		if (line.startsWith("LOC")) {
			// loc = line.substring(4);
			currrentLoc = extractCurrLOC(line);
		}
		return currrentLoc;
	}

	/**
	 * Runs the client
	 */
	public static void main(String[] args) throws Exception {
		ROVER_12 client = new ROVER_12();
		client.run();
	}
}
