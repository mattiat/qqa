package qqa;

import java.sql.ResultSet;

import qqa.be.BEXtractor;
import qqa.db.MysqlConnect;

/** 
 * WARNING: VERY time consuming
 * extract BEs: save answers to files and call BEwT_E on them to generate
 * BE files and equivalence classes. These files are saved in 
 * _QQA/data/BE/final/BEs
 * _QQA/data/BE/final/eqClasses
 * WARNING: the content of those directories will be deleted
 * 
 * @author Mattia Tomasoni
 */
public class D_Extract_BEs {
	/** 
	 *  connection to database
	 */
	private static MysqlConnect database = null;
	
	public static void main(String[] args) throws Exception{
		// keep track of execution time
		long start = System.currentTimeMillis();
		
		// connecting to database
		database = new MysqlConnect();
		
		// retrieve all questions
		String query = "SELECT ques_id, subject, content, content_unprocessed, " +
		"subject_unprocessed FROM FilteredQuestions WHERE " +
		// 50health
//		"ques_id = \'" + "02MVR1DPXLRNK5Z" + "\' OR " +
//		"ques_id = \'" + "0BGCV73MPV4GBDG" + "\' OR " +
//		"ques_id = \'" + "0Z6S2U1FW38BU71" + "\' OR " +
//		"ques_id = \'" + "1JK2OFSYQOC6H24" + "\' OR " +
//		"ques_id = \'" + "1PKEXHVCGC8WBTF" + "\' OR " +
//		"ques_id = \'" + "1S7RCZ4X0FWOKA3" + "\' OR " +
//		"ques_id = \'" + "1UL1IDV7VT5MNDE" + "\' OR " +
//		"ques_id = \'" + "1W44CNT6MRZWKP0" + "\' OR " +
//		"ques_id = \'" + "1YFTOXMATJC2DO4" + "\' OR " +
//		"ques_id = \'" + "211PV1O4LI8FCFJ" + "\' OR " +
//		"ques_id = \'" + "2E1AT26SK8OH16J" + "\' OR " +
//		"ques_id = \'" + "2N47BKL5PPB5L7E" + "\' OR " +
//		"ques_id = \'" + "2OZQEJ88XUAPZ7F" + "\' OR " +
//		"ques_id = \'" + "2XT28GF3YQMMKZP" + "\' OR " +
//		"ques_id = \'" + "32UPRBX5LS1RD3P" + "\' OR " +
//		"ques_id = \'" + "363YQHYWTEI2HTG" + "\' OR " +
//		"ques_id = \'" + "3PHAPMA0277YK84" + "\' OR " +
//		"ques_id = \'" + "3UFWPS56HJVHNLI" + "\' OR " +
//		"ques_id = \'" + "4061V63POXV3C4O" + "\' OR " +
//		"ques_id = \'" + "46P8BQGRB7RRQN7" + "\' OR " +
//		"ques_id = \'" + "4APU8I8BZVC8FCR" + "\' OR " +
//		"ques_id = \'" + "4C77FFRG511DDT3" + "\' OR " +
//		"ques_id = \'" + "4PIMVFM01Y3JGGM" + "\' OR " +
//		"ques_id = \'" + "4UWPVI7CY8R2K7V" + "\' OR " +
//		"ques_id = \'" + "4ZF2LJ4E0SC08OU" + "\' OR " +
//		"ques_id = \'" + "51QAUFUQ368V0EC" + "\' OR " +
//		"ques_id = \'" + "5413X2IMOIPR582" + "\' OR " +
//		"ques_id = \'" + "5E4JR2OZEE4ULQV" + "\' OR " +
//		"ques_id = \'" + "5MGYIUPYATK41Y0" + "\' OR " +
//		"ques_id = \'" + "5MUFHHTWHG6ZNP6" + "\' OR " +
//		"ques_id = \'" + "5MWR0HX5V54D68R" + "\' OR " +
//		"ques_id = \'" + "61OXLMOMT62UWQP" + "\' OR " +
//		"ques_id = \'" + "66AVRHXXTE21OX1" + "\' OR " +
//		"ques_id = \'" + "6CNWHIWWU1YZKKQ" + "\' OR " +
//		"ques_id = \'" + "6JPIMNKC80KVYEH" + "\' OR " +
//		"ques_id = \'" + "6M0VA1XFNAFPRV5" + "\' OR " +
//		"ques_id = \'" + "6X3RKWJ2Q13MM38" + "\' OR " +
//		"ques_id = \'" + "6XI2ZCOLPWP1D45" + "\' OR " +
//		"ques_id = \'" + "710N1LQCH32TLY0" + "\' OR " +
//		"ques_id = \'" + "71PQG5OMISJXDPT" + "\' OR " +
//		"ques_id = \'" + "74CNVBTGACLWF1Q" + "\' OR " +
//		"ques_id = \'" + "7B8J1G5DYLKZQA3" + "\' OR " +
//		"ques_id = \'" + "7GQ7LZ8S3HYPP8Y" + "\' OR " +
//		"ques_id = \'" + "8KZHIA8Q0A88H58" + "\' OR " +
//		"ques_id = \'" + "A83SX008HWMS0XQ" + "\' OR " +
//		"ques_id = \'" + "2S7R7JX1L1KFUPF" + "\' OR " +
//		"ques_id = \'" + "3S5YFQILITB5MP8" + "\' OR " +
//		"ques_id = \'" + "4SXENM8ONTSHW8R" + "\'";
		// 50health2
		"ques_id = \'" + "CIGOYWVSIG0KY5T" + "\' OR " +
		"ques_id = \'" + "CXT8NIR13SMN375" + "\' OR " +
		"ques_id = \'" + "DACSFF55HIBUM1E" + "\' OR " +
		"ques_id = \'" + "DAMFNI0V8URCRPW" + "\' OR " +
		"ques_id = \'" + "DF7HCQW0BPGMVKI" + "\' OR " +
		"ques_id = \'" + "DO04QGGOLPGYTO8" + "\' OR " +
		"ques_id = \'" + "DW11IUHMNSNXEIV" + "\' OR " +
		"ques_id = \'" + "DZLWIZLKGYF5O7D" + "\' OR " +
		"ques_id = \'" + "E8ZMD5RANBBQPU0" + "\' OR " +
		"ques_id = \'" + "EFJCO7DNJVXOOOD" + "\' OR " +
		"ques_id = \'" + "EKT4PRFBFX6O4WV" + "\' OR " +
		"ques_id = \'" + "EP8AGEDSKQOZR6Q" + "\' OR " +
		"ques_id = \'" + "FOIE25AD62MRJAK" + "\' OR " +
		"ques_id = \'" + "G7HV05JXAQNUENL" + "\' OR " +
		"ques_id = \'" + "GOXVA8ARC64ZZBB" + "\' OR " +
		"ques_id = \'" + "HFPGNWFIHYMPN65" + "\' OR " +
		"ques_id = \'" + "HSJGZHCBE7IFHCE" + "\' OR " +
		"ques_id = \'" + "I2G4FX1OYODCW62" + "\' OR " +
		"ques_id = \'" + "IK52OXAMYRSMHCV" + "\' OR " +
		"ques_id = \'" + "IQLNP0BSVMXYHR2" + "\' OR " +
		"ques_id = \'" + "IT7NLUP8DQKF7ZQ" + "\' OR " +
		"ques_id = \'" + "J5X6QZXLNVW033R" + "\' OR " +
		"ques_id = \'" + "JOHD3MIBCEQHOLC" + "\' OR " +
		"ques_id = \'" + "JVW7Q5UNIKSLMK7" + "\' OR " +
		"ques_id = \'" + "KLS11IDEEGGECLF" + "\' OR " +
		"ques_id = \'" + "LLCNZ8P1IV1UY6W" + "\' OR " +
		"ques_id = \'" + "LXAPWPR4DFEG7N4" + "\' OR " +
		"ques_id = \'" + "M12M0C4JMHGELVO" + "\' OR " +
		"ques_id = \'" + "ME3ROWKPKDJHFOT" + "\' OR " +
		"ques_id = \'" + "MR53S2PUF3PF1SE" + "\' OR " +
		"ques_id = \'" + "MVE6O4O7QWVUWVX" + "\' OR " +
		"ques_id = \'" + "N550MBK07XYONDK" + "\' OR " +
		"ques_id = \'" + "N73UQIBEPT5HYJ1" + "\' OR " +
		"ques_id = \'" + "NZRNU8RXP18E2ER" + "\' OR " +
		"ques_id = \'" + "OAOUKIIS06NBGIZ" + "\' OR " +
		"ques_id = \'" + "OEQTNG2QD2CJVP2" + "\' OR " +
		"ques_id = \'" + "OF386LLXZH8YIW1" + "\' OR " +
		"ques_id = \'" + "OJ8CAAAMD07QY4B" + "\' OR " +
		"ques_id = \'" + "OM0DSM7AEI0DLD6" + "\' OR " +
		"ques_id = \'" + "OMG772JPRSUC7JB" + "\' OR " +
		"ques_id = \'" + "PEXRTW72GBUYAL3" + "\' OR " +
		"ques_id = \'" + "PGA16BD07ZYSFC6" + "\' OR " +
		"ques_id = \'" + "PJZLPANYDUGQ7G4" + "\' OR " +
		"ques_id = \'" + "Q53NA8OBNUXNAZI" + "\' OR " +
		"ques_id = \'" + "Q84AVNCQB3IZ5AE" + "\' OR " +
		"ques_id = \'" + "QGGGAAP71M3TZMO" + "\' OR " +
		"ques_id = \'" + "QZ2IE5IGWL63S22" + "\' OR " +
		"ques_id = \'" + "R23NHLGWFQY1SDL" + "\' OR " +
		"ques_id = \'" + "RPN44C7TZO8T3O8" + "\' OR " +
		"ques_id = \'" + "S77M2DIMZQBGYHZ" + "\' OR " +
		"ques_id = \'" + "SFJ5IYPGUMY7Y1E" + "\' OR " +
		"ques_id = \'" + "SHD4O0IGJ71S47Y" + "\' OR " +
		"ques_id = \'" + "T2SPUKCYSFUHX5R" + "\' OR " +
		"ques_id = \'" + "TK704O7YGSHGSCY" + "\' OR " +
		"ques_id = \'" + "TO4Q3TOPHHKNQC1" + "\' OR " +
		"ques_id = \'" + "TST0ODPWY2OHT3H" + "\' OR " +
		"ques_id = \'" + "TXQBZ2XRU05ARCD" + "\'";
		ResultSet filteredQuestions = database.ask(query);
		
		///////////////////////////////////////////////////////////////////////
		// build BE table
		///////////////////////////////////////////////////////////////////////
		// extract BEs and stores results to file in QQA/data/BE/final/BEs and 
		// QQA/data/BE/final/eqClasses.
		// WARNING: VERY time consuming
		// use it once and then rely on QQA/data/BE/final/BEs/* and 
		// QQA/data/BE/final/eqClasses/*
		System.out.println("building BE representation of dataset.");
		System.out.println("might take a LONG time. Please be patient.");
		extractBEs(filteredQuestions);
		
		// disconnecting from database 
		database.disconnect();
		System.out.println("...FINISHED...");
		float elapsedTime = (System.currentTimeMillis()-start)/60000F;
		System.out.println("elapsed time " + elapsedTime + " min");
	}
	
	/**
	 * WARNING: VERY time consuming
	 * extract BEs: save answers to files and call BEwT_E on them to generate
	 * BE files and equivalence classes. These files are saved in 
	 * _QQA/data/BE/final/BEs
	 * _QQA/data/BE/final/eqClasses
	 * WARNING: this method deletes the content of those directories
	 * @param answers
	 * @return
	 * @throws Exception
	 */
	public static void extractBEs(ResultSet questions) 
	throws Exception{
		// create a BE extractor to apply BEwT_E steps to question and answers
		BEXtractor BEXt = new BEXtractor();
		// one question at a time
		questions.beforeFirst();
		while (questions.next()) {
			// Step1; parameter is false if the document is a question
			BEXt.Step1(questions, false);
			// Step2
			BEXt.Step2();
			// Step3
			BEXt.Step3();
			// process all answers to this question
			String query = "SELECT answer_id, ques_id, content, " +
			"content_unprocessed, user_id, isbest FROM FilteredAnswers " +
			"WHERE ques_id=\'"+ questions.getString("ques_id") +"\'";
			ResultSet answers = database.ask(query);
			// better one answer at a time, the process is very RAM consuming!
			answers.beforeFirst();
			while (answers.next()) {
				// Step1; parameter is true if the document is an answer
				BEXt.Step1(answers, true);
				// Step2
				BEXt.Step2();
				// Step3
				BEXt.Step3();
			}
		}
	}
}
