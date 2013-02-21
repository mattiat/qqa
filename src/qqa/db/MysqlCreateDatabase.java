package qqa.db;

import java.sql.*;

public class MysqlCreateDatabase{
	public static void createAnswersTable(MysqlConnect database) 
	throws SQLException {
		database.conn.createStatement().executeUpdate(
				"drop table if exists Answers");
		String answers = "CREATE TABLE " +
		"Answers(" +
		"answer_id INT NOT NULL AUTO_INCREMENT, " +
		"ques_id VARCHAR(15), " +
		"content MEDIUMTEXT, " +
		"content_unprocessed MEDIUMTEXT, " +
		"isbest TINYINT, " +
		"user_id VARCHAR(15), " +
		"datetime DATETIME, " +
		"thumb_up SMALLINT, " +
		"thumb_down SMALLINT, " +
		"PRIMARY KEY (answer_id)" +
		")";
		database.execute(answers);
	}

	public static void createCategoriesTable(MysqlConnect database) 
	throws SQLException {
		database.conn.createStatement().executeUpdate(
				"drop table if exists Categories");
		String categories = "CREATE TABLE " +
		"Categories(" +
		"category_id VARCHAR(15) NOT NULL, " +
		"category_name tinytext, " +
		"PRIMARY KEY (category_id)" +
		")";
		database.execute(categories);
	}

	public static void createQuestionsTable(MysqlConnect database) 
	throws SQLException {
		database.conn.createStatement().executeUpdate(
				"drop table if exists Questions");
		String questions = "CREATE TABLE " +
		"Questions(" +
		"ques_id VARCHAR(15) NOT NULL, " +
		"subject TEXT, " +
		"subject_unprocessed TEXT, " +
		"content MEDIUMTEXT, " +
		"content_unprocessed MEDIUMTEXT, " +
		"datetime DATETIME, " +
		"link tinytext, " +
		"category_id VARCHAR(15), " + 
		"category_name tinytext, " +
		"user_id VARCHAR(15), " +
		"stars SMALLINT, " +
		"closed_by TINYTEXT, " +
		"asker_rating SMALLINT, " +
		"voter_rating SMALLINT, " +
		"num_comments SMALLINT, " +
		"award_time DATETIME, " +
		"PRIMARY KEY (ques_id)" +
		")";
		database.execute(questions);
	}

	public static void createUsersTable(MysqlConnect database) 
	throws SQLException {
		database.conn.createStatement().executeUpdate(
				"drop table if exists Users");
		String users = "CREATE TABLE " +
		"Users(" +
		"user_id VARCHAR(15) NOT NULL, " +
		"total_points INT, " +
		"questions_asked INT, " +
		"questions_resolved INT, " +
		"total_answers INT, " +
		"best_answers INT, " +
		"stars_received INT, " +
		"member_since DATETIME, " +
		"PRIMARY KEY (user_id)" +
		")";
		database.execute(users);
	}
	
	public static void createFeaturesTable(MysqlConnect database) 
	throws SQLException {
		database.conn.createStatement().executeUpdate(
				"drop table if exists Features");
		String features = "CREATE TABLE " +
		"Features(" +
		"isbest FLOAT, " +
		"answer_id VARCHAR(50) NOT NULL, " +
		"ques_id VARCHAR(15) NOT NULL, " +
		"answer_length INT, " +
		"meaningful_words INT, " +
		"answerer_points INT, " +
		"best_answers_ratio FLOAT, " +
		"overlap INT, " +
		"PRIMARY KEY (answer_id, ques_id)" +
		")";
		database.execute(features);
	}
	
	public static void createTermsTable(MysqlConnect database) 
	throws SQLException {
		database.conn.createStatement().executeUpdate(
				"drop table if exists Terms");
		String features = "CREATE TABLE " +
		"Terms(" +
		"answer_id VARCHAR(50) NOT NULL, " +
		"term VARCHAR(50) NOT NULL, " +
		"document_freq INT, " +
		"corpus_freq INT DEFAULT 0, " +
		"PRIMARY KEY (answer_id, term)" +
		")";
		database.execute(features);
	}
	
	public static void createWeightsTable(MysqlConnect database) 
	throws SQLException {
		database.conn.createStatement().executeUpdate(
				"drop table if exists Weights");
		String features = "CREATE TABLE " +
		"Weights(" +
		"isselected FLOAT, " +
		"sentence_id VARCHAR(50) NOT NULL, " +
		"ques_id VARCHAR(15) NOT NULL, " +
		"quality FLOAT, " +
		"coverage FLOAT, " +
		"relevance FLOAT, " +
		"novelty FLOAT, " +
		"sentence_length INT " +
		")";
		database.execute(features);
	}

}
