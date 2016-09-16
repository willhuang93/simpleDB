import java.io.*;
import java.util.*;
import java.text.*;
import java.math.*;
import java.util.regex.*;

public class Solution {

    public static void main(String args[] ) throws Exception {
        /* Enter your code here. Read input from STDIN. Print output to STDOUT */
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        Database database = new Database();
        Database.Transaction baseTransaction = database.new Transaction();
        Database.Transaction currentTransaction = baseTransaction.begin();

        // start
        while(true) {
            String input = stdin.readLine();
            String[] line = input.split("\\s+");
            String command = line[0];
            if (command.equals("END")) {
                System.out.println(input);
                break;
            }
            else if (command.equals("SET")) {
                System.out.println(input);
                currentTransaction.setValue(line[1], line[2]);
            }
            else if (command.equals("GET")) {
                System.out.println(input);
                System.out.printf("> %S\n", database.getValue(line[1]));
            }
            else if (command.equals("UNSET")) {
                System.out.println(input);
                currentTransaction.unsetValue(line[1]);
            }
            else if (command.equals("NUMEQUALTO")) {
                System.out.println(input);
                System.out.printf("> %d\n", database.numEqualTo(line[1]));
            }
            else if (command.equals("BEGIN")) {
                System.out.println(input);
                Database.Transaction newTransaction = database.new Transaction();
                currentTransaction = newTransaction.begin();
            }
            else if (command.equals("ROLLBACK")) {
                System.out.println(input);
                Database.Transaction previousTransaction = database.rollback();
                currentTransaction = (previousTransaction == null) ? baseTransaction : previousTransaction;
            }
            else if (command.equals("COMMIT")) {
                System.out.println(input);
                database.commit();
                currentTransaction = baseTransaction.begin();
            }   
        }
    }

}

class Database {
        protected Map <String, String> db;
        protected Map <String, Integer> valueCount;
        protected List <Transaction> transactions;

        public Database() {
            db = new HashMap <String, String>();
            valueCount = new HashMap <String, Integer>();
            transactions = new ArrayList<Transaction>();
        }

        protected void setValue (String name, String value) {
            // changes the old count
            unsetValue(name);

            // updates, or add the new pair into the db
            db.put(name, value);

            // updates the count of the value
            if (valueCount.containsKey(value)) 
                valueCount.put(value, valueCount.get(value) + 1);
            else
                valueCount.put(value, 1);
        }

        // returns value of the variable 'name', or NULL if variable is not set 
        public String getValue(String name) {
            String val = db.get(name);
            if (db.containsKey(name))
                return val;
            else 
                return "NULL";
        }

        // unset the variable 'name'
        protected void unsetValue(String name) {
            String dbValue = db.get(name);
            Integer count = (dbValue != null) ? valueCount.get(dbValue) : -1;
            db.remove(name);

            // if the value has a count of 1, remove the record from the table, else subtract 1 from its count
            if (count == 1) 
                valueCount.remove(dbValue);
            else if (count > 1)
                valueCount.put(dbValue, count - 1);
        }

        public Integer numEqualTo(String value) {
            Integer val = valueCount.get(value);
            return (val == null) ? 0 : val; 
        }

        // rollback basically performs the opposite of the commands stored in the actions arraylist
        public Transaction rollback() {
            int transactionCount = transactions.size();

            if (transactionCount == 1) {
                System.out.println("> NO TRANSACTION");
                return null;
            }
            else {
                Transaction currentTransaction = transactions.get(transactions.size() - 1);

                // gets the current transaction's dirty writes
                List<String> actionsList = currentTransaction.actions;

                // iterates through them from the back
                for (int i = actionsList.size() - 1; i >= 0; i--) {
                    String[] action = actionsList.get(i).split("\\s+");
                    if (action[0].equals("SET")) {
                        // unsets the data, and adjusts the count for the value
                        unsetValue(action[1]);

                        // handles case of inserting a new non-existent key during transaction,
                        if (action[2] != null)
                            // sets the data, and adjusts the count back to the original
                            setValue(action[1], action[2]);

                    } 
                    else if (action[0].equals("UNSET")) 
                        // sets the data, and adjusts the count back to the original
                        setValue(action[1], action[2]);
                }

                // closes the current transaction
                transactions.remove(currentTransaction);

                // returns the previous transaction if there is one
                return (transactions.size() == 1) ? null : transactions.get(transactions.size() - 1);
            }
        }

        public void commit() {
            int transactionCount = transactions.size();
            
            if (transactionCount == 1)
                System.out.println("> NO TRANSACTION");
            
            transactions.clear();
        }

        // transaction class that keeps track of actions performed in current transaction blocks,
        // also keeps modifies transactionCount in the database class during each initialization
        class Transaction {
            public List<String> actions;

            public Transaction() {
                actions = new ArrayList<String>();
            }

            public void setValue (String name, String value) {
                if (transactions.size() > 1)
                    //logs the SET command with the name and the original value
                    log("SET", name, db.get(name));
                
                Database.this.setValue(name, value);
            }

            public void unsetValue(String name) {
                if (db.containsKey(name)) { 
                    if (transactions.size() > 1)
                        // logs the UNSET command with the name and the original value
                        log("UNSET", name, db.get(name));

                    Database.this.unsetValue(name);
                }
            }

            // logs the command for this transaction block
            public void log(String action, String name, String value) {
                String newAction = action + " " + name + " " + value;
                actions.add(newAction);
            }

            public Transaction begin() {
                Database.this.transactions.add(this);
                return this;
            }
        }
    }