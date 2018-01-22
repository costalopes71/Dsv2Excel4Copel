package br.com.sinapsis.exportadorcopel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import br.com.sinapsis.exportadorcopel.model.Alimentador;
import br.com.sinapsis.exportadorcopel.model.Medicao;
import br.com.sinapsis.exportadorcopel.model.Subestacao;
import br.com.sinapsis.exportadorcopel.util.Utils;

/**
 * Classe responsável por exportar os dados de um arquivo DSV da empresa Copel que contém as medições de seus alimentadores
 * e criar um arquivo Excel (xlsx) com um determinado template inserindo essas medições. Esse arquivo excel é usado
 * posteriormente pelo programa ConversorDeMedicoes para gerar o arquivo MDB que posteriormente irá popular um banco de dados.
 * @author Joao Lopes
 * @since 10/01/2018
 * @version 1.0b
 */
public class ExportadorCopel {

	private SXSSFWorkbook wb;
	private BufferedReader br;
	private String dsvPath;
	private String excelPath;
	private long startTime;
	private HashMap<Integer, Integer> mapPosicaoAlimentores;
	private HashMap<Integer, HashMap<String, Integer>> posicoesLinha;

	public ExportadorCopel(String dsvPath, String excelPath) throws FileNotFoundException {
		wb = new SXSSFWorkbook(-1);
		this.dsvPath = dsvPath;
		this.excelPath = excelPath;
		startTime = System.currentTimeMillis();
		mapPosicaoAlimentores = new HashMap<>();
		posicoesLinha = new HashMap<>();
	}

	/**
	 * Método que gera um arquivo excel para a empresa Copel a partir de um arquivo DSV.
	 * @param hasHeader <code>boolean</code>, booleano que indica se o arquivo DSV possui cabeçalho.
	 * @throws IOException
	 * @throws ParseException
	 */
	public void generateExcelFile(boolean hasHeader) throws IOException, ParseException {
		
		this.generateExcelTemplate(hasHeader);
		this.orderDsvFileBySubstation();
		
		long start = System.currentTimeMillis();
		System.out.println("----------------------------------------------------------------");
		System.out.println("Initiating the data write on the file.");
		System.out.println("----------------------------------------------------------------");
		System.out.println("\nWritting data, please wait. This can take several minutes depending on the size of the file\n");

		br = Utils.openBufferedReader(dsvPath);
		Utils.hasHeader(true, br);
		
		String line;
		String lastSubstation = "";
		while ((line = br.readLine()) != null) {
			
			if (line.endsWith("|")) {
				line = line + "0";
			}
			
			String[] fields = line.split("\\|");
			
			/*esta condicao serve para quando mudar a subestacao (uma vez que o arquivo DSV foi ordenado por subestacao pelo
			método orderDsvFileBySubstation() ) fazer o flush daquela subestacao (ou seja, daquela sheet uma vez que cada
			sheet é um subestacao), isto é feito para evitar o estouro de memória (é feito a cada subestacao pq uma vez que
			as linhas sao flushadas elas não estão mais acessíveis em memória).*/
			if (!lastSubstation.equals(fields[4]) && !lastSubstation.equals("")) {
				SXSSFSheet sheetToFlush = wb.getSheet(lastSubstation);
				sheetToFlush.flushRows();
			}
			
			int codigoSub = Integer.parseInt(fields[7]);
			Medicao medicao = this.setMedicaoFromFields(fields);
			SXSSFSheet sheet = wb.getSheet(fields[4]);
			
			int initialCell = mapPosicaoAlimentores.get(Integer.parseInt(fields[0]));
			int rowNum = 0;
			
			if (posicoesLinha.containsKey(codigoSub)) {
				if (posicoesLinha.get(codigoSub).containsKey(fields[6])) {
					rowNum = posicoesLinha.get(codigoSub).get(fields[6]);
					Row row = sheet.getRow(rowNum);
					this.writeMedicao(row, medicao, initialCell);
				} else {
					Row newRow = this.writeRow(sheet, medicao);
					rowNum = newRow.getRowNum();
					this.writeMedicao(newRow, medicao, initialCell);
					posicoesLinha.get(codigoSub).put(fields[6], rowNum);
				}
			} else {
				Row newRow = this.writeRow(sheet, medicao);
				rowNum = newRow.getRowNum();
				this.writeMedicao(newRow, medicao, initialCell);
				HashMap<String, Integer> aux = new HashMap<>();
				aux.put(fields[6], rowNum);
				posicoesLinha.put(codigoSub, aux);
			}
			
			lastSubstation = fields[4];
		}
		
		this.save(wb, excelPath);
		br.close();
		
		double endTime = System.currentTimeMillis();
		double elapsed = endTime - start;
		double totalElapsedTime = endTime - startTime;
		
		System.out.println("----------------------------------------------------------------");
		System.out.println("SUCCESS: data written successfully .");
		System.out.println("Elapsed time: " + (elapsed / 1000)+ " seconds.");
		System.out.println("----------------------------------------------------------------\n");
		
		System.out.println("----------------------------------------------------------------");
		System.out.println("Thank you for using ExportadorCopel.");
		System.out.println("Elapsed TOTAL time: " + (totalElapsedTime / 1000)+ " seconds.");
		System.out.println("----------------------------------------------------------------\n");
	}
	
	/**
	 * Método que gera o template excel onde os dados deverão posteriormente serem inseridos. Para tanto, este método
	 * lê o arquivo DSV da empresa Copel e para cada Subestação cria uma planilha (aba) no arquivo excel. Cria também
	 * o cabeçalho de cada planilha onde estarão a data, a hora da medição e os alimentadores (com suas respectivas
	 * medições) daquela Subestacao.
	 * @param hasHeader
	 * @throws IOException
	 */
	private void generateExcelTemplate(boolean hasHeader) throws IOException {

		long start = System.currentTimeMillis();
		System.out.println("----------------------------------------------------------------");
		System.out.println("Initiating the generation of the TEMPLATE file.");
		System.out.println("----------------------------------------------------------------");

		br = Utils.openBufferedReader(dsvPath);
		HashMap<Integer, Subestacao> subMap = new HashMap<>();
		Utils.hasHeader(hasHeader, br);

		String line;
		while ((line = br.readLine()) != null) {
			String[] fields = line.split("\\|");
			 /* Testa se no mapa ja existe a subestacao do registro que acabou de ser lido. Se não tiver ele cria um 
			  * novo objeto subestacao, seta o seu atributo codigo (codigo da subestacao no arquivo é o índice 3 do 
			  * vetor de campos), cria um objeto Alimentador e seta o seu codigo (codigo do alimentador no arquivo é 
			  * o índice 1 do vetor de campos), pega o atributo alimentadores (HashSet) do objeto subestacao que foi 
			  * criado e adiciona ao HashSet o alimentador. Finalmente, adiciona no HashMap subMap (linha 69 e 70) o 
			  * objeto subestacao. */
			if (!subMap.containsKey(Integer.parseInt(fields[7]))) {
				Subestacao subAux = new Subestacao();
				subAux.setCodigo(Integer.parseInt(fields[7]));
				subAux.setSigla(fields[4]);
				Alimentador alimentador = new Alimentador();
				alimentador.setCodigo(Integer.parseInt(fields[0]));
				alimentador.setSigla(fields[2]);
				subAux.getAlimentadores().add(alimentador);
				subMap.put(subAux.getCodigo(), subAux);
				/* Se a subestacao ja existir testa se o alimentador daquele registro lido já existe dentro do HashSet da
				subestacao, se nao existir, adiciona. */
			} else {
				Subestacao sub = subMap.get(Integer.parseInt(fields[7]));
				HashSet<Alimentador> alimentadorSet = sub.getAlimentadores();
				if (!alimentadorSet.contains(Integer.parseInt(fields[0]))) {
					Alimentador aux = new Alimentador();
					aux.setCodigo(Integer.parseInt(fields[0]));
					aux.setSigla(fields[2]);
					alimentadorSet.add(aux);
					sub.setAlimentadores(alimentadorSet);
					subMap.put(sub.getCodigo(), sub);
				}
			}
		}
		
		br.close();
		this.createSheets(subMap);
		double elapsed = System.currentTimeMillis() - start;

		System.out.println("----------------------------------------------------------------");
		System.out.println("SUCCESS: Template file generated.");
		System.out.println("Elapsed time: " + (elapsed / 1000) + " seconds.");
		System.out.println("----------------------------------------------------------------");
		
	}

	/**
	 * Método responsável por ordenar o arquivo DSV da empresa COPEL por subestação. Gera um novo arquivo DSV no mesmo
	 * caminho que o DSV original e com o mesmo nome mudando apenas o final com _ORDERED. Este novo arquivo será usado
	 * pelo método principal <code>generateExcelFile</code> para escrever os dados no arquivo excel sem causar problema
	 * de memória (out of memory).
	 * @throws IOException
	 */
	private void orderDsvFileBySubstation() throws IOException {
		
		long start = System.currentTimeMillis();
		System.out.println("----------------------------------------------------------------");
		System.out.println("Initiating the generation of the ordered by substation DSV file.");
		System.out.println("----------------------------------------------------------------");
		
		br = Utils.openBufferedReader(dsvPath);
		String orderedDsvPath = this.createOrderedDsvPath();
		PrintWriter pw = Utils.createPrintWriter(orderedDsvPath);
		HashMap<Integer, ArrayList<String>> mapSub = new HashMap<>();
		String header = br.readLine();
		pw.println(header);
		String line = null;
		
		while ((line = br.readLine()) != null) {
			String[] fields = line.split("\\|");
			int codSub = Integer.parseInt(fields[7]);
			if (mapSub.containsKey(codSub)) {
				mapSub.get(codSub).add(line);
			} else {
				ArrayList<String> aux = new ArrayList<>();
				aux.add(line);
				mapSub.put(codSub, aux);
			}
		}
		
		for (Map.Entry<Integer, ArrayList<String>> mapValue : mapSub.entrySet()) {
			ArrayList<String> aux = mapValue.getValue();
			for (int i = 0; i < aux.size(); i++) {
				pw.println(aux.get(i));
			}
		}
		
		br.close();
		pw.close();
		this.dsvPath = orderedDsvPath;
		
		double elapsed = System.currentTimeMillis() - start;
		System.out.println("----------------------------------------------------------------");
		System.out.println("SUCCESS: Ordered file generated.");
		System.out.println("Elapsed time: " + (elapsed / 1000) + " seconds.");
		System.out.println("----------------------------------------------------------------");
		
	}
	
	private String createOrderedDsvPath() {
		return dsvPath.substring(0, dsvPath.lastIndexOf(".")) + "_ORDERED.csv";
	}

	private void createSheets(HashMap<Integer, Subestacao> subMap) {
		for (Map.Entry<Integer, Subestacao> mapValue : subMap.entrySet()) {
			Subestacao subestacao = mapValue.getValue();
			SXSSFSheet sheet = wb.createSheet(subestacao.getSigla());
			Row rowHeader = sheet.createRow(0);
			createHeaderSheet(rowHeader, subestacao);
		}
	}
	
	private void createHeaderSheet(Row rowHeader, Subestacao subestacao) {
		this.createStaticHeaderFields(rowHeader);
		int cellCounter = 2;
		int qtAlim = subestacao.getAlimentadores().size();
		
		for (Alimentador alimentador : subestacao.getAlimentadores()) {
			
			if (!mapPosicaoAlimentores.containsKey(alimentador.getCodigo())) {
				mapPosicaoAlimentores.put(alimentador.getCodigo(), cellCounter);
			}
			
			rowHeader.createCell(cellCounter).setCellValue(subestacao.getSigla() + "_" + alimentador.getSigla() + "_IA");
			cellCounter++;
			rowHeader.createCell(cellCounter).setCellValue(subestacao.getSigla() + "_" + alimentador.getSigla() + "_IB");
			cellCounter++;
			rowHeader.createCell(cellCounter).setCellValue(subestacao.getSigla() + "_" + alimentador.getSigla() + "_IC");
			cellCounter++;
			rowHeader.createCell(cellCounter).setCellValue(subestacao.getSigla() + "_" + alimentador.getSigla() + "_POT_ATIVA");
			cellCounter++;
			rowHeader.createCell(cellCounter).setCellValue(subestacao.getSigla() + "_" + alimentador.getSigla() + "_POT_REAT");
			cellCounter++;
			rowHeader.createCell(cellCounter).setCellValue(subestacao.getSigla() + "_" + alimentador.getSigla() + "_FATOR_POT");
			cellCounter++;
			rowHeader.createCell(cellCounter).setCellValue(subestacao.getSigla() + "_" + alimentador.getSigla() + "_TENSAOA");
			cellCounter++;
			rowHeader.createCell(cellCounter).setCellValue(subestacao.getSigla() + "_" + alimentador.getSigla() + "_TENSAOB");
			cellCounter++;
			rowHeader.createCell(cellCounter).setCellValue(subestacao.getSigla() + "_" + alimentador.getSigla() + "_TENSAOC");
			cellCounter++;
		}
		this.makeHeaderBold(rowHeader, qtAlim);
	}

	private void makeHeaderBold(Row rowHeader, int qtAlim) {
		CellStyle style = wb.createCellStyle();
		Font font = wb.createFont();
		font.setBold(true);
		style.setFont(font);
		for (int i = 0; i < 2 + (qtAlim * 9); i++) {
			rowHeader.getCell(i).setCellStyle(style);
		}
	}
	
	private void createStaticHeaderFields(Row rowHeader) {
		rowHeader.createCell(0).setCellValue("DATA");
		rowHeader.createCell(1).setCellValue("HORA");
	}

	private Medicao setMedicaoFromFields(String[] fields) throws ParseException {
		Medicao medicao = new Medicao();
		Utils.replaceNullAndEmptyFields(fields);
		medicao.setData(fields[6]+ "00");
		medicao.setCorrFaseA(Integer.parseInt(fields[9]));
		medicao.setCorrFaseB(Integer.parseInt(fields[10]));
		medicao.setCorrFaseC(Integer.parseInt(fields[11]));
		medicao.setPotAtiva(Integer.parseInt(fields[12]));
		medicao.setPotReat(Integer.parseInt(fields[13]));
		medicao.setFatorPot(Double.parseDouble(fields[14].replace(',', '.')));
		medicao.setTensaoA(Double.parseDouble(fields[15].replace(',', '.')));
		medicao.setTensaoB(Double.parseDouble(fields[16].replace(',', '.')));
		medicao.setTensaoC(Double.parseDouble(fields[17].replace(',', '.')));
		return medicao;
	}
	
	private void writeMedicao(Row newRow, Medicao medicao, int initialCell) {
		newRow.createCell(initialCell).setCellValue(medicao.getCorrFaseA());
		initialCell++;
		newRow.createCell(initialCell).setCellValue(medicao.getCorrFaseB());
		initialCell++;
		newRow.createCell(initialCell).setCellValue(medicao.getCorrFaseC());
		initialCell++;
		newRow.createCell(initialCell).setCellValue(medicao.getPotAtiva());
		initialCell++;
		newRow.createCell(initialCell).setCellValue(medicao.getPotReat());
		initialCell++;
		newRow.createCell(initialCell).setCellValue(medicao.getFatorPot());
		initialCell++;
		newRow.createCell(initialCell).setCellValue(medicao.getTensaoA());
		initialCell++;
		newRow.createCell(initialCell).setCellValue(medicao.getTensaoB());
		initialCell++;
		newRow.createCell(initialCell).setCellValue(medicao.getTensaoC());
	}
	
	@SuppressWarnings("unused")
	private int findInitialCell(String alimentadorCode, SXSSFSheet sheet) {
		Row headerRow = sheet.getRow(0);
		for (Cell cell : headerRow) {
			if (cell.getRichStringCellValue().getString().contains(alimentadorCode)) {
				return cell.getColumnIndex();
			}
		}
		return -1;
	}
	
	/**
	 * 
	 * @param workbook
	 * @param excelPath
	 * @throws IOException
	 */
	public void save(SXSSFWorkbook workbook, String excelPath) throws IOException {
		try {
			FileOutputStream outputStream = new FileOutputStream(new File(excelPath));
			workbook.write(outputStream);
			outputStream.close();
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("Destiny path not found.");
		} catch (IOException e) {
			throw new IOException("Error when editing the file.");
		}
	}

	private Row writeRow(SXSSFSheet sheet, Medicao medicao) {
		Row newRow = sheet.createRow(sheet.getLastRowNum() + 1);
		newRow.createCell(0).setCellValue(Utils.formatDatePattern(medicao.getData()));
		newRow.createCell(1).setCellValue(Utils.formatHourPattern(medicao.getData()));
		return newRow;
	}
	
}
