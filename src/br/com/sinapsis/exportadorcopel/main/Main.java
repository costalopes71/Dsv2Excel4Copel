package br.com.sinapsis.exportadorcopel.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;

import br.com.sinapsis.exportadorcopel.ExportadorCopel;

public class Main {

	public static void main(String[] args) throws ParseException {
		
		String originFilePath = "C:/Users/usrsnp/Documents/Joao/ArquivoModeloCopel/LEITURA_ALIMENTADOR_NOVO.csv";
		String destinyFilePath = "C:/Users/usrsnp/Documents/Joao/ArquivoModeloCopel/processed_excel_file_TESTE.xlsx";
		
		try {
			ExportadorCopel e = new ExportadorCopel(originFilePath, destinyFilePath);
			e.generateExcelFile(true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
	}
	
}
