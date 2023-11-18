import static spark.Spark.*;

import java.util.ArrayList;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class Main {

	private static Gson gson = new Gson();

	//private static ArrayList<Order> orders = new ArrayList<Order>();
	
	public static void connect() {
		Connection conn = null;
		try {
			// DB path
			String url = "jdbc:sqlite:orderserver.db";

			// SQL statement for creating the orders table if it isn't exist
			String sql = "CREATE TABLE IF NOT EXISTS orders (\n" 
			        + "	orderID INTEGER PRIMARY KEY,\n"
					+ "	bookID INT NOT NULL\n"
					+ ");";

			// create a connection to the database
			conn = DriverManager.getConnection(url);
			System.out.println("Connection to SQLite has been established.");

			Statement stmt = conn.createStatement();

			// create a new table
			stmt.execute(sql);

			// close the connection with the DB
			conn.close();
		} catch (SQLException e) {
			System.out.println("Error in connect fun: " + e.getMessage());
		}
	}
	
	public static int addNewOrder(int bookID) {
		
		Connection conn = null;
		
		int orderID = -1;
		
		try {
			// DB path
			String url = "jdbc:sqlite:orderserver.db";			

			// the insert query
			String SQLquery = "insert into orders (bookID) values('" + bookID + "');";
			
			// create a connection to the database
			conn = DriverManager.getConnection(url);

			Statement stmt = conn.createStatement();
			
			// create new order
			stmt.executeUpdate(SQLquery);
			
			// query to select the last added order to get its ID which generated by the SQLite
			SQLquery = "SELECT orderID from orders order by orderID desc limit 1";
			
			ResultSet result = stmt.executeQuery(SQLquery);
			
			if (result.next()) {
				// extract the ID of the order added to the DB
	            orderID = result.getInt("orderID");
	        }

			conn.close();
		} catch (SQLException e) {
			System.out.println("Error in add fun: " + e.getMessage());
		}
		
		// return the ID of the added order
		return orderID;
	}
	
	public static void main(String[] args) {
		
		// connect to the DB
		connect();
		
		// the purchase API
		post("/purchase/:bookId", (req, res) -> {
			
			// extract the 'bookID' value from the URL
			String requestId = req.params(":bookID");
			
			Book book = new Book();
					    
			try {
	            // URL of the info API we want to call
	            String apiUrl = "http://localhost:4568/info/" + requestId;

	            // open a connection to the info API
	            URL url = new URL(apiUrl);
	            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	            connection.setRequestMethod("GET");

	            // extract the response code
	            int responseCode = connection.getResponseCode();

	            // check if the response code indicates not found (HTTP 404 OK)
	            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
	            	return "There is no book with ID = " + requestId;
	            }
	            
	            // read the info API response
	            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	            StringBuilder responseStringBuilder = new StringBuilder();
	            String line;
	            while ((line = reader.readLine()) != null) {
	                responseStringBuilder.append(line);
	            }
	            reader.close();

	            // get the response as a string
	            String apiResponse = responseStringBuilder.toString();

	            // converting the response to a book object
	            book = gson.fromJson(apiResponse, Book.class);
	            
	            // check if the quantity is zero
	            if(book.getQuantity() <= 0) {
	            	return "The item is out of stock";
	            }
	            
	            // URL of the dec API that will be called to decrement the quantity
	            apiUrl = "http://localhost:4568/dec/" + book.getId();
	            url = new URL(apiUrl);
	            connection = (HttpURLConnection) url.openConnection();
	            connection.setRequestMethod("PUT");

	            // extract the response code
	            int responseCode2 = connection.getResponseCode();

	            // Read the response content
	            BufferedReader reader2 = new BufferedReader(new InputStreamReader(connection.getInputStream())); 
	            StringBuilder response = new StringBuilder();
	            String line2;
	            while ((line2 = reader2.readLine()) != null) {
	                response.append(line2);
	            }
	            reader2.close();	            
	            
	            // add the order to the DB
	            int orderID = addNewOrder(book.getId());
	            
	            // if the order isn't added successfully
	            if(orderID == -1) {
	            	return "Request failed";
	            }
	            
	            // return the id of the created order
	            return "Purchase done successfully! \nThe order id = " + orderID;
	            
	        } catch (IOException e) {
	            e.printStackTrace();
	            res.status(500); // Internal Server Error
	            return "Error calling the API";
	        }
		});
	}
}
