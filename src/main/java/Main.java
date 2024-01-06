import static spark.Spark.*;

import java.util.ArrayList;

import com.google.gson.Gson;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class Main {

	private static Gson gson = new Gson();

	// private static ArrayList<Book> catalog = new ArrayList<Book>();

	public static void connect() {
		Connection conn = null;
		try {
			// DB path
			String url = "jdbc:sqlite:catalog.db";

			// SQL statement for creating the book table if it isn't exist
			String sql = "CREATE TABLE IF NOT EXISTS book (\n" 
			        + "	bookID INT NOT NULL,\n"
					+ "	topic VARCHAR(500) NOT NULL,\n" + "	title VARCHAR(500) NOT NULL,\n" + "	price INT NOT NULL,\n"
					+ "	quantity INT NOT NULL,\n" + " PRIMARY KEY (bookID)" + ");";

			// create a connection to the database
			conn = DriverManager.getConnection(url);
			System.out.println("Connection to SQLite has been established.");

			Statement stmt = conn.createStatement();

			// create a new table
			stmt.execute(sql);

			// insert books into the database
//          String SQLquery = "insert into book values('1', 'distributed systems', 'How to get a good grade in DOS in 40 minutes a day', '30', '10');";
//          stmt.executeUpdate(SQLquery);
//			SQLquery = "insert into book values ('2', 'distributed systems', 'RPCs for Noobs', '30', '10');";
//			stmt.executeUpdate(SQLquery);
//			SQLquery = "insert into book values ('3', 'undergraduate school', 'Xen and the Art of Surviving Undergraduate School', '30', '10');";
//			stmt.executeUpdate(SQLquery);
//			SQLquery = "insert into book values('4', 'undergraduate school', 'Cooking for the Impatient Undergrad', '30', '10');";
//			stmt.executeUpdate(SQLquery);

//			String SQLquery = "insert into book values('5', 'new topic', 'How to finish Project 3 on time', '30', '10');";
//			stmt.executeUpdate(SQLquery);
//			SQLquery = "insert into book values ('6', 'new topic', 'Why theory classes are so hard', '30', '10');";
//			stmt.executeUpdate(SQLquery);
//			SQLquery = "insert into book values ('7', 'new topic', 'Spring in the Pioneer Valley', '30', '10');";
//			stmt.executeUpdate(SQLquery);
		
			// close the connection with the DB
			conn.close();
			
		} catch (SQLException e) {
			System.out.println("Error from catalog server: " + e.getMessage());
		}
	}
	
	public static void main(String[] args) {

		// change the port of the catalog server to prevent conflict with the order server
		port(4568);
		
		// connect to the DB
		connect();

		// search API
		get("/search/:topic", (req, res) -> {
			
			// extract the 'topic' value from the URL
			String encodedTopic = req.params(":topic");
		    String decodedTopic = URLDecoder.decode(encodedTopic, StandardCharsets.UTF_8.toString());
			
			ArrayList<BookDTO> books = new ArrayList<BookDTO>();
			
			String sql = "SELECT * From Book";
			
			try {
				String url = "jdbc:sqlite:catalog.db";
				Connection conn = DriverManager.getConnection(url);
				Statement stmt = conn.createStatement();
				ResultSet result = stmt.executeQuery(sql);

				// loop through the result to find the books with the requested topic
				while (result.next()) {
					String bookID = result.getString("bookID");
					String topic = result.getString("topic");
					String title = result.getString("title");
					if (topic.equals(decodedTopic)) {
						books.add(new BookDTO(Integer.parseInt(bookID), title));
					}
				}
				conn.close();
			} catch (SQLException e) {
				System.out.println("Error from search API " + e.getMessage());
			}

			// if there is no book with the requested topic
			if(books.isEmpty()) {
				res.status(404);
				return "There is no book with the topic: " + decodedTopic;
			}
			return gson.toJson(books);
		});

		// info API
		get("/info/:bookID", (req, res) -> {

			// extract the 'bookID' value from the URL
			int requestId = Integer.parseInt(req.params(":bookID"));

			String sql = "SELECT * From Book";
			
			try {
				String url = "jdbc:sqlite:catalog.db";
				Connection conn = DriverManager.getConnection(url);
				Statement stmt = conn.createStatement();
				ResultSet result = stmt.executeQuery(sql);				
				
				// looping through the result to find the book with the requested id
				while (result.next()) {
					int bookID = result.getInt("bookID");
					String topic = result.getString("topic");
					String title = result.getString("title");
					int quantity = result.getInt("quantity");
					int price = result.getInt("price");
					if (bookID == requestId) {
						Book book = new Book(bookID, topic, title, price, quantity);
						conn.close();
						return gson.toJson(book);
					}
				}
				conn.close();
			} catch (SQLException e) {
				System.out.println("Error from info API " + e.getMessage());
			}

			// if there is no book with the requested id
			res.status(404);
			return "There is no book with id = " + requestId;
		});
		
		// the decrement API (to decrement the quantity of a specific book)
		put("/dec/:bookID", (req, res) -> {
			
			// extract the 'bookID' value from the URL
			String requestedID = req.params(":bookID");
			
			// the update query to decrement the quantity
			String updateStatement = "UPDATE book SET quantity = quantity - 1 WHERE bookID = ?;";
			
			// try to execute the update query
			try {
				String url = "jdbc:sqlite:catalog.db";
				Connection conn = DriverManager.getConnection(url);
				PreparedStatement pstmt = conn.prepareStatement(updateStatement);
				pstmt.setInt(1, Integer.parseInt(requestedID));
				pstmt.executeUpdate();
				conn.close();
			} catch (SQLException e) {
				System.out.println("Error from dec API " + e.getMessage());
			}
			
			return "Book's quantity updated successfully";
		});
	}
}
