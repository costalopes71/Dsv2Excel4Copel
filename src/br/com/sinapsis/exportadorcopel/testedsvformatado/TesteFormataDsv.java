//package br.com.sinapsis.exportadorcopel.testedsvformatado;
//
//import java.io.BufferedReader;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//
//import br.com.sinapsis.exportadorcopel.util.Utils;
//
//public class TesteFormataDsv {
//
//	private Utils util;
//	private static BufferedReader br;
//	private String dsvPath;
//	
//	public TesteFormataDsv(String dsvPath) {
//		util = new Utils();
//		this.dsvPath = dsvPath;
//	}
//	
//	public static void main(String[] args) throws IOException {
//		
//		HashMap<Integer, String[]> teste = createReference("C:/Users/usrsnp/Documents/Joao/ArquivoModeloCopel/SubAlim.csv");
//		
//		for (Map.Entry<Integer, String[]> mapValue : teste.entrySet()) {
//			System.out.println(mapValue.getKey());
//			String[] aux = mapValue.getValue();
//			for (int i = 0; i < aux.length; i++) {
//				System.out.println(aux[i]);
//			}
//			System.out.println("=================================");
//		}
//		
//		System.out.println(teste.size());
//	}
//	
//	public static HashMap<Integer, String[]> createReference(String referenceFile) throws IOException {
//		
//		HashMap<Integer, String[]> referenceMap = new HashMap<>();
//		openBufferedReader(referenceFile);
//		
//		br.readLine();
//		
//		String line;
//		while ((line = br.readLine()) != null) {
//			String[] referenceFields = line.split(";");
//			String[] alimentadorData = {referenceFields[3], referenceFields[2]};
//			referenceMap.put(Integer.parseInt(referenceFields[4]), alimentadorData);
//		}
//		
//		return referenceMap;
//	}
//	
//	public void formatDsvFile() throws IOException {
//		
//		long start = System.currentTimeMillis();
//		System.out.println("----------------------------------------------------------------");
//		System.out.println("Initiating the generation of the ordered by substation DSV file.");
//		System.out.println("----------------------------------------------------------------");
//		
//		this.openBufferedReader();
//		String orderedDsvPath = this.createOrderedDsvPath();
//		PrintWriter pw = util.createPrintWriter(orderedDsvPath);
//		HashMap<Integer, ArrayList<String>> mapSub = new HashMap<>();
//		String header = br.readLine();
//		pw.println(header);
//		String line = null;
//		
//		while ((line = br.readLine()) != null) {
//			String[] fields = line.split("\\|");
//			int codSub = Integer.parseInt(fields[3]);
//			if (mapSub.containsKey(codSub)) {
//				mapSub.get(codSub).add(line);
//			} else {
//				ArrayList<String> aux = new ArrayList<>();
//				aux.add(line);
//				mapSub.put(codSub, aux);
//			}
//		}
//		
//		for (Map.Entry<Integer, ArrayList<String>> mapValue : mapSub.entrySet()) {
//			ArrayList<String> aux = mapValue.getValue();
//			for (int i = 0; i < aux.size(); i++) {
//				pw.println(aux.get(i));
//			}
//		}
//		
//		br.close();
//		pw.close();
//		this.dsvPath = orderedDsvPath;
//		
//		double elapsed = System.currentTimeMillis() - start;
//		System.out.println("----------------------------------------------------------------");
//		System.out.println("SUCCESS: Ordered file generated.");
//		System.out.println("Elapsed time: " + (elapsed / 1000) + " seconds.");
//		System.out.println("----------------------------------------------------------------");
//		
//	}
//
//	private static void openBufferedReader(String referenceFile) throws FileNotFoundException {
//		FileReader inputStream = null;
//		try {
//			inputStream = new FileReader(referenceFile);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//			throw new FileNotFoundException("Reference file not found.");
//		}
//		br = new BufferedReader(inputStream);
//	}
//	
//	private void openBufferedReader() throws FileNotFoundException {
//		FileReader inputStream = null;
//		try {
//			inputStream = new FileReader(dsvPath);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//			throw new FileNotFoundException("DSV file not found.");
//		}
//		br = new BufferedReader(inputStream);
//	}
//
//	private String createOrderedDsvPath() {
//		return dsvPath.substring(0, dsvPath.lastIndexOf(".")) + "_FORMATED.dsv";
//	}
//	
//}
