/**
 * Contains classes to upload the dataset to database
 * 
 */
package qqa.db.dataset;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;

import qqa.db.MysqlConnect;

/**
 * extract dataset information from file and store it into the database
 *
 */
public abstract class Extractor {

	public File file; 
	public static Hashtable<String,String> storedQuesId = new Hashtable<String, String>(); 

	public Extractor(String fileName){
		file = new File(fileName);
	}

	/**
	 * extract information from file, one unit per line, and call store to 
	 * upload it into the database
	 * @param database
	 * @throws IOException
	 * @throws SQLException
	 */
	public void extract(MysqlConnect database) throws IOException, 
	SQLException {
		BufferedReader input =  new BufferedReader(new FileReader(file));
		String line = null; 
		double valid = 0;
		double incomplete = 0;
		double rejected = 0;
		// one line one datum
		while (( line = input.readLine()) != null){
			// removing unwanted unwanted characters from string
			line = line.replace( '"', ' ' );
			line = line.replace( '\'', ' ' );
			line = line.replace( '\\', ' ' );
			// splitting datum into its components
			String [] parts = line.split("\t");
			// check validity of values
			boolean sentinel = true;
			for(int i=0; i<parts.length; i ++){
				if (parts[i].equals("UNKNOWN")){
					sentinel = false;
					parts[i] = "-1";
				}
			}
			if(sentinel) {
				// store values, returns 1 if value was stored, otherwise 0
				if (store(parts, database))
				{
					valid += 1;
					// limit to N answers: qqa_test database option
					// if (valid==100 && this instanceof AnswerExtractor) break;
				}
				else
					rejected += 1;
			}
			else incomplete++;
		}
		System.out.println((int) (valid) + " out of " + 
				(int) (valid + incomplete + rejected) + " values were inserted (" + 
				(int) (incomplete/(valid + incomplete + rejected)*100) + 
				"% were incomplete and " +
				(int) (rejected/(valid + incomplete + rejected)*100) + 
				"% were rejected)");
	}

	/**
	 * store into the database: dynamically chosen according to the information
	 *  being treated: answers, rather then questions, categories or users
	 * @param parts
	 * @param database
	 * @return
	 * @throws SQLException
	 */
	public abstract boolean store(String [] parts, MysqlConnect database) throws SQLException;
}
