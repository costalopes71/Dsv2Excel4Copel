package br.com.sinapsis.exportadorcopel.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;

/**
 * Classe 
 * @author Joao Lopes
 *
 */
public final class Utils {

	private Utils() {
	
	}
	
	/**
	 * Método que recebe por parâmetro a data que esta no arquivo DSV da empresa (no formato yyyyMMddHHmm) e transforma 
	 * para uma String que representa a data no formato ddMMyyyy.
	 * @param date <code>String</code>, que representa a data que foi lida no arquivo DSV da empresa.
	 * @return data <code>String</code>, no formato ddMMyyyy
	 */
	public static String formatDatePattern(String date) {
		return date.substring(6, 8) + "/" + date.substring(4, 6) + "/" + date.substring(0, 4);
	}

	/**
	 * Método que recebe por parâmetro a data que esta no arquivo DSV da empresa (no formato yyyyMMddHHmm) e transforma 
	 * para uma String que representa a hora no formato HH:mm.
	 * @param date <code>String</code>, que representa a data que foi lida no arquivo DSV da empresa.
	 * @return hour <code>String</code>, no formato HH:mm.
	 */
	public static String formatHourPattern(String date) {
		return date.substring(8, 10) + ":00";
	}

	/**
	 * Método que varre as linhas de uma planilha e verifica se ja existe alguma
	 * linha com a data lida. Se existir retorna o número da linha da planilha,
	 * se não existe retorna -1.
	 * 
	 * @param sheet
	 *            (XSSFSheet), planilha a ser varrida
	 * @param dataMedicao
	 *            (data a ser procurada)
	 * @return int, número da linha em que existe a data procurada OU -1 se não
	 *         existir.
	 */
	public static int findRow(SXSSFSheet sheet, String dateToFind, String hourToFind) {
		for (Row row : sheet) {
			if (row.getCell(0).getStringCellValue().equals(dateToFind)) {
				if (row.getCell(1).getStringCellValue().equals(hourToFind)) {
					return row.getRowNum();
				}
			}
		}
		return -1;
	}

	/**
	 * Método responsável por pular o cabeçalho do arquivo para começar a leitura das medições. Espera por 2 (dois) 
	 * parâmetros.
	 * @param hasHeader <code>boolean</code>, indicando true se tiver cabeçalho e false para se não tiver cabeçalho.
	 * @param br <code>BufferedReader</code>, objeto BufferedReader que contém as linhas do arquivo lido;
	 * @throws IOException
	 */
	public static void hasHeader(boolean hasHeader, BufferedReader br) throws IOException {
		if (hasHeader) {
			try {
				br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				throw new IOException("ERROR - No lines found.");
			}
		}
	}

	/**
	 * Método responsável por não deixar nenhum campo da medição nulo, para evitar assim exceções do tipo NullPointerException.
	 * Para tanto recebe como parâmetro um array de Strings que representam os campos da medição e o campo que for nulo
	 * é substituído por 0.
	 * @param fields <code>String[]</code>, array de String que representam os campos da medição.
	 */
	public static void replaceNullAndEmptyFields(String[] fields) {
		for (int i = 0; i < fields.length; i++) {
			if (fields[i] == null || fields[i].equals("")) {
				fields[i] = "0";
			}
		}
	}

	public static PrintWriter createPrintWriter(String orderedDsvPath) throws IOException {
		FileWriter outputStream;
		try {
			outputStream = new FileWriter(orderedDsvPath);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException("ERROR - Unable to create or open the new DSV file in this path: " + orderedDsvPath);
		}
		return new PrintWriter(outputStream);
	}

	public static BufferedReader openBufferedReader(String filePath) throws FileNotFoundException {
		FileReader inputStream = null;
		try {
			inputStream = new FileReader(filePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new FileNotFoundException("DSV file not found.");
		}
		return new BufferedReader(inputStream);
	}

}