package rover_kae;

/*  scan map size is 11 x 11
 comment */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import swarmBots.ROVER_12;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import common.Coord;
import common.MapTile;
import common.ScanMap;
import enums.Science;
import enums.Terrain;

/**
 * The seed that this program is built on is a chat program example found here:
 * http://cs.lmu.edu/~ray/notes/javanetexamples/ Many thanks to the authors for
 * publishing their code examples
 * 
 * allowed # request to the server per sec = 500 2 req / sec
 */

public class RV_12_ks_current extends ROVER_12 {
	Random rd = new Random();
	Coord currentLoc, previousLoc;
	String currentDir = "";
	int hzDir = 1; // 0 = R to L, 1 = L to R,
	Set<String> blockedDirs = new HashSet<String>();
	Set<String> openDirs = new HashSet<String>();
	String[] cardinals = new String[4];
	MapTile[][] mapJournal = new MapTileUtil[100][100];
	boolean[][] footPrints = new boolean[100][100];

	MapTile[][] tempScanMap;

	public RV_12_ks_current() {
		super();
	}

	public RV_12_ks_current(String serverAddress) {
		super(serverAddress);
	}

	public void run() throws IOException, InterruptedException {

		int rdNum;
		String currentDir;
		boolean stuck = false;

		// TODO - need to close this socket
		makeConnAndInitStream();
		processServerMsgAndWaitForIDRequestCall();
		this.doScan();
		scanMap.debugPrintMap();
		debugPrintMapTileArray(mapJournal);
		System.out.println(currentLoc);
		// Thread.sleep(5000);

		// ******** Rover logic *********
		String[] cardinals = { "E", "S", "W", "N" };
		String line = "";

		ArrayList<String> equipment = getEquipment();
		System.out.println("ROVER_12 equipment list " + equipment + "\n");

		// for debug
		// moveRover12ToAClearArea();
		setCurrentLoc(currentLoc);

		// ******** Rover motion *********
		int waveLength = 6, waveHeight = 4;
		sinusoidal_LR(cardinals, waveLength, waveHeight);
		hzDir = 1;
		int xPosTracker = 0, yPosTracker = 0;
		while (true) {
			doScan();
			debugPrint4Dirs(currentLoc);
			debugPrintMapTileArray(mapJournal);

			// debug
			// moveTowardsSandForDebug();

			previousLoc = currentLoc;
			if (previousLoc.getXpos() == previousLoc.getXpos()) {
				xPosTracker++;
			}
			if (currentLoc.getYpos() == previousLoc.getYpos()) {
				yPosTracker++;
			}
			if (xPosTracker > 10 || yPosTracker > 10) {
				switchDir_sinusoidal();
			}
			{
				doThisWhenStuck_4stepToOpenDir(currentLoc, scanMap.getScanMap());
			}
	
			debugPrintMapTileArray(mapJournal);
			//Thread.sleep(5000);

			//random(cardinals);
			Thread.sleep(sleepTime);

			System.out
					.println("\nROVER_12 ------------ bottom process control --------------");
		}
	}

	private void switchDir_sinusoidal() throws InterruptedException,
			IOException {
		int waveLength = 6, waveHeight = 4;
		if (hzDir == 1) {
			sinusoidal_RL(cardinals, waveLength, waveHeight);
			hzDir = 0;
		} else {
			sinusoidal_LR(cardinals, waveLength, waveHeight);
			hzDir = 1;
		}
	}

	private void doThisWhenStuck_4stepToOpenDir(Coord currentLoc,
			MapTile[][] scanMapTiles) throws InterruptedException, IOException {

		String currentDir;
		findOpenDirs(currentLoc);
		currentDir = openDirs.toArray(new String[1])[0];
		for (int i = 0; i < 4; i++) {
			move(currentDir);
			Thread.sleep(300);
		}
	}

	private void doThisWhenStuck(Coord currentLoc, MapTile[][] scanMapTiles)
			throws InterruptedException, IOException {

		for (int i = 0; i < 4; i++) {
			aStepAwayFromClutter();
			Thread.sleep(300);
		}
	}

	private void makeConnAndInitStream() throws UnknownHostException,
			IOException {
		Socket socket = new Socket(SERVER_ADDRESS, PORT_ADDRESS); // set port
																	// here
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
	}

	private void processServerMsgAndWaitForIDRequestCall() throws IOException {
		while (true) {
			String line = in.readLine();
			if (line.startsWith("SUBMITNAME")) {
				out.println(rovername);
				break;
			}
		}
	}

	private void resetOpenDir() {
		openDirs.add("E");
		openDirs.add("W");
		openDirs.add("S");
		openDirs.add("N");
	}

	private void findOpenDirs(Coord currentLoc) {
		// KSTD - do I need to run findBlockedDirs every time I do getopendir()?
		resetOpenDir();
		findBlockedDirs(currentLoc);

		// DEBUG - remove before submission
		System.out.print("\n" + "BLOCK CHECK:");
		System.out.print("\n" + "blocked:");
		for (String s : blockedDirs) {
			System.out.print(s + " ");
		}

		for (String dir : blockedDirs) {
			if (blockedDirs.contains(dir)) {
				openDirs.remove(dir);
			}
		}

		// DEBUG - remove before submission
		System.out.print("\n" + "open:");
		for (String s : openDirs) {
			System.out.print(s + " ");
		}
	}

	// ******* currently developing ****************
	// KSTD - implement
	private boolean isTreasureSpot() {

		return false;
	}

	private boolean isSand(String dir) throws IOException {

		int x = 5, y = 5; // half of the scan range

		// debug
		System.out.println("from isSand()\n"
				+ "e: "
				+ scanMap.getScanMap()[y][x + 1].getTerrain().equals(
						Terrain.SAND)
				+ "\tw: "
				+ scanMap.getScanMap()[y][x - 1].getTerrain().equals(
						Terrain.SAND)
				+ "\tn: "
				+ scanMap.getScanMap()[y - 1][x].getTerrain().equals(
						Terrain.SAND)
				+ "\ts: "
				+ scanMap.getScanMap()[y + 1][x].getTerrain().equals(
						Terrain.SAND));

		if (dir.equals("E")) {
			return scanMap.getScanMap()[y][x + 1].getTerrain().equals(
					Terrain.SAND);
		}
		if (dir.equals("W")) {
			return scanMap.getScanMap()[y][x - 1].getTerrain().equals(
					Terrain.SAND);
		}
		if (dir.equals("N")) {
			return scanMap.getScanMap()[y - 1][x].getTerrain().equals(
					Terrain.SAND);
		}
		if (dir.equals("S")) {
			return scanMap.getScanMap()[y + 1][x].getTerrain().equals(
					Terrain.SAND);
		}

		return true;
	}

	// **********************************************
	// KSTD - implement it
	private void aStepAwayFromClutter() throws IOException {
		findOpenDirs(currentLoc);

		int tracker = 0;
		while (tracker < 1) {
			for (String dir : openDirs) {
				move(dir);
			}
		}
	}

	private void findBlockedDirs(Coord currentLoc) {
		int centerIndex = (scanMap.getEdgeSize() - 1) / 2;

		System.out.println("scan map size ( findBlockedDirs() ): "
				+ scanMap.getEdgeSize());

		tempScanMap = scanMap.getScanMap();

		// debugPrintDirs(scanMapTiles, centerIndex);
		System.out.println("scanMapTiles: " + scanMap.getScanMap());
		if (withinTheGrid(centerIndex, centerIndex - 1, tempScanMap.length)
				&& tempScanMap[centerIndex][centerIndex - 1].getHasRover()
				|| tempScanMap[centerIndex][centerIndex - 1].getTerrain() == Terrain.ROCK
				|| tempScanMap[centerIndex][centerIndex - 1].getTerrain() == Terrain.NONE
				|| tempScanMap[centerIndex][centerIndex - 1].getTerrain() == Terrain.SAND) {
			System.out.println("north blocked");
			blockedDirs.add("N");
		}

		if (withinTheGrid(centerIndex, centerIndex + 1, tempScanMap.length)
				&& tempScanMap[centerIndex][centerIndex + 1].getHasRover()
				|| tempScanMap[centerIndex][centerIndex + 1].getTerrain() == Terrain.ROCK
				|| tempScanMap[centerIndex][centerIndex + 1].getTerrain() == Terrain.NONE
				|| tempScanMap[centerIndex][centerIndex + 1].getTerrain() == Terrain.SAND) {
			System.out.println("south blocked");
			blockedDirs.add("S");
		}

		if (withinTheGrid(centerIndex + 1, centerIndex, tempScanMap.length)
				&& tempScanMap[centerIndex + 1][centerIndex].getHasRover()
				|| tempScanMap[centerIndex + 1][centerIndex].getTerrain() == Terrain.ROCK
				|| tempScanMap[centerIndex + 1][centerIndex].getTerrain() == Terrain.NONE
				|| tempScanMap[centerIndex + 1][centerIndex].getTerrain() == Terrain.SAND) {
			System.out.println("east blocked");
			blockedDirs.add("E");
		}

		if (withinTheGrid(centerIndex - 1, centerIndex, tempScanMap.length)
				&& tempScanMap[centerIndex - 1][centerIndex].getHasRover()
				|| tempScanMap[centerIndex - 1][centerIndex].getTerrain() == Terrain.ROCK
				|| tempScanMap[centerIndex - 1][centerIndex].getTerrain() == Terrain.NONE
				|| tempScanMap[centerIndex - 1][centerIndex].getTerrain() == Terrain.SAND) {
			System.out.println("west blocked");
			blockedDirs.add("W");
		}

	}

	private void debugPrintDirs(MapTile[][] scanMapTiles, int centerIndex) {
		System.out.println("center: "
				+ scanMapTiles[centerIndex][centerIndex].getHasRover());
		System.out
				.println("s: "
						+ scanMapTiles[centerIndex][centerIndex + 1]
								.getElevation()
						+ " "
						+ scanMapTiles[centerIndex][centerIndex + 1]
								.getHasRover()
						+ " "
						+ scanMapTiles[centerIndex][centerIndex + 1]
								.getTerrain()
						+ " "
						+ scanMapTiles[centerIndex][centerIndex + 1]
								.getScience());
		System.out
				.println("n: "
						+ scanMapTiles[centerIndex][centerIndex - 1]
								.getElevation()
						+ " "
						+ scanMapTiles[centerIndex][centerIndex - 1]
								.getHasRover()
						+ " "
						+ scanMapTiles[centerIndex][centerIndex - 1]
								.getTerrain()
						+ " "
						+ scanMapTiles[centerIndex][centerIndex - 1]
								.getScience());
		System.out
				.println("w: "
						+ scanMapTiles[centerIndex - 1][centerIndex]
								.getElevation()
						+ " "
						+ scanMapTiles[centerIndex - 1][centerIndex]
								.getHasRover()
						+ " "
						+ scanMapTiles[centerIndex - 1][centerIndex]
								.getTerrain()
						+ " "
						+ scanMapTiles[centerIndex - 1][centerIndex]
								.getScience());
		System.out
				.println("e: "
						+ scanMapTiles[centerIndex + 1][centerIndex]
								.getElevation()
						+ " "
						+ scanMapTiles[centerIndex + 1][centerIndex]
								.getHasRover()
						+ " "
						+ scanMapTiles[centerIndex + 1][centerIndex]
								.getTerrain()
						+ " "
						+ scanMapTiles[centerIndex + 1][centerIndex]
								.getScience());

		blockedDirs.clear();
		if (currentDir.equals("E")) {
			if (scanMapTiles[centerIndex][centerIndex + 1].getHasRover()
					|| scanMapTiles[centerIndex][centerIndex + 1].getTerrain() == Terrain.ROCK
					|| scanMapTiles[centerIndex][centerIndex + 1].getTerrain() == Terrain.NONE
					|| scanMapTiles[centerIndex][centerIndex + 1].getTerrain() == Terrain.SAND) {
				System.out.println("east blocked");
				blockedDirs.add("E");
			}
		}
	}

	// TODO - we will not
	private void harvestScience() {
	}

	private MapTile[][] pullLocalMap() throws IOException {

		MapTile[][] scanMapTiles = scanMap.getScanMap();
		return scanMapTiles;
	}

	private void setCurrentLoc(Coord loc) throws IOException {
		String line;
		out.println("LOC");
		line = in.readLine();
		if (line == null) {
			// System.out.println("ROVER_12 check connection to server");
			line = "";
		}
		if (line.startsWith("LOC")) {
			// loc = line.substring(4);
			currentLoc = extractLOC(line);
		}
	}

	private void snake(String[] cardinals, int scanRange) {
		// TODO Auto-generated method stub

	}

	private void sinusoidal(String[] cardinals) throws InterruptedException,
			IOException {

		int waveLength = 3, waveHeight = 6, steps = waveLength;
		cardinals[0] = "E";
		cardinals[1] = "S";
		cardinals[2] = "E";
		cardinals[3] = "N";
		for (int i = 0; i < cardinals.length; i++) {

			currentDir = cardinals[i];
			if (currentDir.equals("E") || currentDir.equals("W")) {
				steps = waveLength;
			} else {
				steps = waveHeight;
			}

			for (int j = 0; j < steps; j++) {
				move(currentDir);
				Thread.sleep(300);
			}
		}
	}

	private boolean isStuck(Coord curr, Coord prev) {
		return curr.equals(prev);
	}

	

	private void move(String dir) throws IOException {
		System.out.println("current location in move(): " + currentLoc);
		setCurrentLoc(currentLoc);
		doScanOriginal();

		switch (dir) {

		case "E":
			if (!isSand("E")) {
				System.out.println("request move -> E");
				moveEast();
			}
			break;
		case "W":
			if (!isSand("W")) {
				System.out.println("request move -> W");
				moveWest();
			}
			break;
		case "N":
			if (!isSand("N")) {
				System.out.println("request move -> N");
				moveNorth();
			}
			break;
		case "S":
			if (!isSand("S")) {
				System.out.println("request move -> S");
				moveSouth();
			}
			break;
		default:
			break;
		}
	}

	private void moveEast() throws IOException {

		out.println("MOVE E");
		System.out.print(currentLoc + " - E -> ");
		System.out.print(currentLoc + "\n");
	}

	private void moveWest() throws IOException {
		out.println("MOVE W");
		System.out.print(currentLoc + " - W -> ");
		System.out.print(currentLoc + "\n");
	}

	private void moveNorth() throws IOException {
		out.println("MOVE N");
		System.out.print(currentLoc + " - N -> ");
		System.out.print(currentLoc + "\n");

	}

	private void moveSouth() throws IOException {
		out.println("MOVE S");
		System.out.print(currentLoc + " - S -> ");
		System.out.print(currentLoc + "\n");

	}
	private void sinusoidal_LR(String[] cardinals, int waveLength,
			int waveHeight) throws InterruptedException, IOException {
		
		int numSteps = waveLength;
		String currentDir;
		cardinals[0] = "E";
		cardinals[1] = "S";
		cardinals[2] = "E";
		cardinals[3] = "N";
		
		for (int i = 0; i < cardinals.length; i++) {

			currentDir = cardinals[i];
			if (currentDir.equals("E")) {
				numSteps = waveLength;
			} else {
				numSteps = waveHeight;
			}

			for (int j = 0; j < numSteps; j++) {
				move(currentDir);
				Thread.sleep(300);
			}
		}
	}
	private void sinusoidal_RL(String[] cardinals, int waveLength,
			int waveHeight) throws InterruptedException, IOException {
		int numSteps = waveLength;
		String currentDir;
		cardinals[0] = "W";
		cardinals[1] = "S";
		cardinals[2] = "W";
		cardinals[3] = "N";
		
		for (int i = 0; i < cardinals.length; i++) {

			currentDir = cardinals[i];
			if (currentDir.equals("W")) {
				numSteps = waveLength;
			} else {
				numSteps = waveHeight;
			}

			for (int j = 0; j < numSteps; j++) {
				move(currentDir);
				Thread.sleep(300);
			}
		}
	}

	private void moveTowardsSandForDebug() throws IOException {
		for (int i = 0; i < 17; i++) {
			move("E");
		}
		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%5");

		for (int i = 0; i < 5; i++) {
			move("S");
		}

		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%5");
		previousLoc = currentLoc;

	}

	private void random(String[] cardinals) throws InterruptedException,
			IOException {
		int rdNum;
		String currentDir;
		for (int i = 0; i < 5; i++) {
			rdNum = randomNum(0, 3);
			currentDir = cardinals[rdNum];

			for (int j = 0; j < 3; j++) {
				move(currentDir);
				Thread.sleep(300);
			}
		}
	}

	private void moveRover12ToAClearArea() throws InterruptedException,
			IOException {
		for (int i = 0; i < 5; i++) {
			move("E");
			Thread.sleep(700);
		}
		for (int i = 0; i < 5; i++) {
			// get out of the crowd of rovers
			move("S");
			Thread.sleep(300);
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
		System.out.println("ROVER_12 method getEquipment()");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		out.println("EQUIPMENT");

		String jsonEqListIn = in.readLine(); // grabs the string that was
												// returned first
		if (jsonEqListIn == null) {
			jsonEqListIn = "";
		}
		StringBuilder jsonEqList = new StringBuilder();
		System.out
				.println("ROVER_12 incomming EQUIPMENT result - first readline: "
						+ jsonEqListIn);

		if (jsonEqListIn.startsWith("EQUIPMENT")) {
			while (!(jsonEqListIn = in.readLine()).equals("EQUIPMENT_END")) {
				if (jsonEqListIn == null) {
					break;
				}
				// System.out.println("ROVER_12 incomming EQUIPMENT result: " +
				// jsonEqListIn);
				jsonEqList.append(jsonEqListIn);
				jsonEqList.append("\n");
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

	public void initMapJournal() {
		for (int i = 0; i < mapJournal.length; i++) {
			for (int j = 0; j < mapJournal[i].length; j++) {

			}
		}
	}

	// sends a SCAN request to the server and puts the result in the scanMap
	// array
	public void doScan() throws IOException {
		System.out.println("ROVER_12 method doScan()");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		out.println("SCAN");

		// grabs the string that was returned first
		String jsonScanMapIn = in.readLine();

		if (jsonScanMapIn == null) {
			System.out.println("ROVER_12 check connection to server");
			jsonScanMapIn = "";
		}
		StringBuilder jsonScanMap = new StringBuilder();
		// System.out.println("ROVER_12 incomming SCAN result - first readline: "
		// + jsonScanMapIn);

		if (jsonScanMapIn.startsWith("SCAN")) {
			while (!(jsonScanMapIn = in.readLine()).equals("SCAN_END")) {
				jsonScanMap.append(jsonScanMapIn);
				jsonScanMap.append("\n");
			}
		} else {
			// in case the server call gives unexpected results
			clearReadLineBuffer();
			return; // server response did not start with "SCAN"
		}

		String jsonScanMapString = jsonScanMap.toString();

		setCurrentLoc(currentLoc);
		// convert from the json string back to a ScanMap object
		scanMap = gson.fromJson(jsonScanMapString, ScanMap.class);

		// set the pointer object to currently scanned ScanMap
		MapTile[][] ptrScanMap = scanMap.getScanMap();

		Terrain ter;
		Science sci;
		int elev;
		boolean hasR;

		int scanMapHalfSize = (int) Math.floor(ptrScanMap.length / 2.);

		// set top left corner of the section of the map on the global map
		// journal
		Coord start = new Coord(currentLoc.getXpos() - scanMapHalfSize,
				currentLoc.getYpos() - scanMapHalfSize);

		// debug
		// System.out.println("scanMap: ");
		// debugPrintMapTileArray(ptrScanMap);

		// FIXME - there's a problem with the map copy
		for (int i = 0; i < ptrScanMap.length; i++) {
			for (int j = 0; j < ptrScanMap.length; j++) {

				if (withinTheGrid(start.ypos + i, start.xpos + j,
						mapJournal.length)) {
					ter = ptrScanMap[i][j].getTerrain();
					sci = ptrScanMap[i][j].getScience();
					elev = ptrScanMap[i][j].getElevation();
					hasR = ptrScanMap[i][j].getHasRover();

					if (mapJournal[start.ypos + i][start.xpos + j] == null) {
						mapJournal[start.ypos + i][start.xpos + j] = new MapTileUtil(
								ter, sci, elev, hasR);
					}
				}
			}
		}

		debugPrintMapTileArray(mapJournal);
	}

	public void doScanOriginal() throws IOException {
		Gson gson = new GsonBuilder().setPrettyPrinting()
				.enableComplexMapKeySerialization().create();
		out.println("SCAN");

		String jsonScanMapIn = in.readLine();
		if (jsonScanMapIn == null) {
			System.out.println("ROVER_12 check connection to server");
			jsonScanMapIn = "";
		}
		StringBuilder jsonScanMap = new StringBuilder();

		if (jsonScanMapIn.startsWith("SCAN")) {
			while (!(jsonScanMapIn = in.readLine()).equals("SCAN_END")) {
				jsonScanMap.append(jsonScanMapIn);
				jsonScanMap.append("\n");
			}
		} else {
			// in case the server call gives unexpected results
			clearReadLineBuffer();
			return; // server response did not start with "SCAN"
		}

		String jsonScanMapString = jsonScanMap.toString();
		scanMap = gson.fromJson(jsonScanMapString, ScanMap.class);

	}

	public boolean withinTheGrid(int i, int j, int arrayLength) {
		return i >= 0 && j >= 0 && i < arrayLength && j < arrayLength;
	}

	// this takes the LOC response string, parses out the x and y values and
	// returns a Coord object
	public static Coord extractLOC(String sStr) {
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

	public int randomNum(int min, int max) {
		return rd.nextInt(max + 1) + min;
	}

	/**
	 * Runs the client
	 */
	public static void main(String[] args) throws Exception {
		RV_12_ks_current client = new RV_12_ks_current();
		client.run();
	}

	// TODO - incomplete
	private void debugPrint4Dirs(Coord currLoc) {
		// System.out.println("center: "+
		// getScanMap().[currLoc.getYpos()][currLoc.getXpos()]);
		scanMap.debugPrintMap();
	}

	public void debugPrintMapTileArray(MapTile[][] mapTileArray) {

		int edgeSize = mapTileArray.length;
		System.out.println("edge size: " + edgeSize);
		for (int k = 0; k < edgeSize + 2; k++) {
			System.out.print("--");
		}

		System.out.print("\n");

		for (int j = 0; j < edgeSize; j++) {

			System.out.print("j=" + j + "\t");

			System.out.print("| ");
			for (int i = 0; i < edgeSize; i++) {
				if (mapTileArray[i][j] == null) {
					System.out.print("n");
				}
				// check and print edge of map has first priority
				else if (mapTileArray[i][j].getTerrain().toString()
						.equals("NONE")) {
					System.out.print("XX");

					// next most important - print terrain and/or science
					// locations
					// terrain and science
				} else if (!(mapTileArray[i][j].getTerrain().toString()
						.equals("SOIL"))
						&& !(mapTileArray[i][j].getScience().toString()
								.equals("NONE"))) {
					// both terrain and science

					System.out.print(mapTileArray[i][j].getTerrain().toString()
							.substring(0, 1)
							+ mapTileArray[i][j].getScience().getSciString());
					// just terrain
				} else if (!(mapTileArray[i][j].getTerrain().toString()
						.equals("SOIL"))) {
					System.out.print(mapTileArray[i][j].getTerrain().toString()
							.substring(0, 1)
							+ " ");
					// just science
				} else if (!(mapTileArray[i][j].getScience().toString()
						.equals("NONE"))) {
					System.out.print(" "
							+ mapTileArray[i][j].getScience().getSciString());

					// if still empty check for rovers and print them
				} else if (mapTileArray[i][j].getHasRover()) {
					System.out.print("[]");

					// nothing here so print nothing
				} else {
					System.out.print("  ");
				}
			}
			System.out.print(" |\n");
		}
		for (int k = 0; k < edgeSize + 2; k++) {
			System.out.print("--");
		}
		System.out.print("\n");
	}
}