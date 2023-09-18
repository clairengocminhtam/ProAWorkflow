package com.beanlib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

//the class for handling all workflow activities
public class Workflow {
	int row = 0;
	// all assessment related information in the units table
	final String[] at_label = {"at1", "at2", "at3"};
 	final String[] at_status = {"at1_status", "at2_status", "at3_status"};
 	final String[] at_feedback = {"at1_feedback", "at2_feedback", "at3_feedback"};
 	final String[] at_type = {"at1_type", "at2_type", "at3_type"};
 	final String[] reviewer1_at = {"reviewer1_at1", "reviewer1_at2", "reviewer1_at3"};
	final String[] reviewer2_at = {"reviewer2_at1", "reviewer2_at2", "reviewer2_at3"};
	final String[] reviewer3_at = {"reviewer3_at1", "reviewer3_at2", "reviewer3_at3"};		

	/* set the offered units to be reviewed in the database and 
	 * return discipline head email addresses so that they will be informed to allocate stakeholders
	 * Sprint 2 is to complete this method 
	 * Hints: 
	 *   1. Declare a HashSet object to store unique discipline head email addresses
	 *   2. use the same sql query in norminate_units.jsp to retrieve all units with 
	 *   	options ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE so that you can update the table
	 *   3. Use a while (rs.next()) loop to check whether the current row number (using variable row) is in the offered array 
	 *   	(using index i). If yes, the '2boffered' column of the 'units' table is set to 1 and advance index i. 
	 *   4. Further check whether the row is also in the reviewed array (using index j). If also yes, 
	 *   	the '2breviewed' column of the 'units' table is set to 1 and advance index j. For the unit to be reviewed, 
	 *   	retrieve its discipline head email addresses using method getEmailAddresses(sqlquery, "") and add them to the HashSet.
	 *   	The sqlquery should have conditions specifying which discipline (discipline=??) and the dh role (roles LIKE '%dh%'). 
	 *   	Finally update the entire row of the table.
	 *   5. Convert the HashSet into an array and return it.
	 * 
	 */
	public String[] setReviewedUnits(String[] offered, String[] reviewed) {
        // using HashSet to store unique discipline head email 
        // addresses (potentially more than one person)
        
        Set<String> dhs = new HashSet<String>(); 
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(Configuration.dbConnectionURL);
            PreparedStatement ps=conn.prepareStatement("SELECT * FROM proadb.units ORDER BY discipline ASC, code ASC", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);    
            ResultSet rs = ps.executeQuery();
            
            
            int row = 0;
            int i = 0;
            int j = 0;
            
            while(rs.next()){
                if( Integer.parseInt(offered[i]) == row) {
                    rs.updateInt("2boffered",1);
                    i++;
                    if(Integer.parseInt(reviewed[j]) == row) {
                        rs.updateInt("2breviewed", 1);
                        j++;
                      
                      String Discipline = rs.getString("discipline");
                      String sqlquery = "SELECT * FROM proadb.login WHERE discipline='" + Discipline + "' AND roles Like '%dh%'"; 
                      
                      String[] c = getEmailAddresses(sqlquery, "");
                      
                       for(int g = 0 ; g < c.length ; g++) {
                           dhs.add(c[g]);
                       }
                    }
                }
                rs.updateRow();
                row++;   
            }
            rs.close();
            ps.close();
            conn.close();
            
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        // convert HashSet to an array
        String[] simpleArray = new String[dhs.size()];
        dhs.toArray(simpleArray);
        
        return simpleArray;
        
    } 
	
	// Store NLiC and reviewer for each unit to be reviewed 
	// Called by DH
	public Boolean setStakeholders(String disc, String sqlquery, String[] nlics, String[] reviewer1, String[] reviewer2, String[] reviewer3) {
		// whether the information is stored successfully
		Boolean updateSuccess = false;

		try {
			// load and register JDBC driver for MySQL
			Class.forName("com.mysql.cj.jdbc.Driver");
			Connection conn = DriverManager.getConnection(Configuration.dbConnectionURL);
			PreparedStatement ps=conn.prepareStatement(sqlquery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE); 

			ResultSet rs = ps.executeQuery();
			// write each element in the arrays to the corresponding rows returned by the query
			int i = 0;
			while (rs.next() && i < reviewer1.length) {
				rs.updateString("nlic", nlics[i]);
				rs.updateString("reviewer1", reviewer1[i]);

				if (reviewer2[i] != null && reviewer2[i] != "") 
					rs.updateString("reviewer2", reviewer2[i]);

				if (reviewer3[i] != null && reviewer3[i] != "") 
					rs.updateString("reviewer3", reviewer3[i]);

				rs.updateRow();
				i++;
				updateSuccess = true;
			}

			rs.close();
			ps.close();
			conn.close();

		} catch(Exception e) {
			e.printStackTrace();
		}

		return updateSuccess;

	}

	//retrieve relevant units using sqlquery
	public DBResult getUnits(String sqlquery) {
		DBResult dbr = null;
		
		try {
			// load and register JDBC driver for MySQL
			Class.forName("com.mysql.cj.jdbc.Driver");
			Connection conn = DriverManager.getConnection(Configuration.dbConnectionURL);
			PreparedStatement ps=conn.prepareStatement(sqlquery); 
			ResultSet rs = ps.executeQuery();
			
			dbr = new DBResult(conn, ps, rs);
						
		} catch(Exception e) {
	         //Handle errors for Class.forName
	         e.printStackTrace();
		}
		return dbr;
		
	}
	
	// return a user's discipline based on its login email
	public String getDiscipline(String email) {
		String discipline = "";
		
		try {
			// load and register JDBC driver for MySQL
			Class.forName("com.mysql.cj.jdbc.Driver");
			Connection conn = DriverManager.getConnection(Configuration.dbConnectionURL);
			PreparedStatement ps=conn.prepareStatement("select * from login where email=?"); 
			ps.setString(1, email);
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				discipline = rs.getString("discipline");
			}
						
		} catch(Exception e) {
	         //Handle errors for Class.forName
	         e.printStackTrace();
		}
		
		return discipline;
		
	}
	
	// return the email addresses depending on sqlquery and role
	public String[] getEmailAddresses(String sqlquery, String role) {
			// using HashSet to store unique email 
			// addresses (potentially more than one person)			
			List<String> emailaddrs = new ArrayList<String>();
			
			try {
				// load and register JDBC driver for MySQL
				Class.forName("com.mysql.cj.jdbc.Driver");
				Connection conn = DriverManager.getConnection(Configuration.dbConnectionURL);
				// "SELECT * FROM proadb.units WHERE discipline='" + Discipline + "'"
				PreparedStatement ps=conn.prepareStatement(sqlquery); 
				ResultSet rs = ps.executeQuery();
				
				while (rs.next()) {
					// if role is not specified, all email addresses returned by sqlquery are added
					if (role.equals("")) {
						emailaddrs.add(rs.getString("email"));
					// add email addresses of this specified role 
					} else {
						// value of the field may be null
						String addr = rs.getString(role);
						if (addr != null) 
							emailaddrs.add(addr);
					}
					
				}
							
			} catch(Exception e) {
		         //Handle errors for Class.forName
		         e.printStackTrace();
			}
			// convert HashSet to an array
			String[] simpleArray = new String[emailaddrs.size()];			
			emailaddrs.toArray(simpleArray);
			
			return simpleArray;
			
	}
	
	// set assessment statuses
	/* Each assessment may be in one of the following status after upload:
	 1. uploaded: original assessment uploaded by NLiC
	 2. lt_returned: returned to NLiC via L&T screening due to compliance issues before review
	 3. dh_returned: returned to NLiC via DH checking due to issues after review and revision
	 4. ready: ready for review marked by L&T
	 5. reviewed: reviewed by all reviewers (reviewed) or by some of them (reviewer1/reviewer2/reviewer3)
	 6. revised: revised by NLiC by addressing review comments
	 7. approved: approved by DH for publishing
    */
	public void setAssessmentStatus(String code, List<String> columns, List<String> values) {		
		try {
			// load and register JDBC driver for MySQL
			Class.forName("com.mysql.cj.jdbc.Driver");
			Connection conn = DriverManager.getConnection(Configuration.dbConnectionURL);
			String sqlquery = "SELECT * from units where code= '" + code + "'";
			PreparedStatement ps=conn.prepareStatement(sqlquery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE); 
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				for (int i=0; i<columns.size(); i++) {
					rs.updateString(columns.get(i), values.get(i));
				}  	
	 		    rs.updateRow();
			}
			
			rs.close();
			ps.close();
			conn.close();
						
		} catch(Exception e) {
	         //Handle errors for Class.forName
	         e.printStackTrace();
		}
	}
	
	/* store uploaded/revised assessments into database and set appropriate assessment statuses
	 * Sprints 3 and 4 are to complete this method.
	 * Sprint 3 needs to: (1) set each assessment file type, (2) set each assessment status, 
	 * 		(3) upload each assessment by nlic and upload each reviewed assessment by a pr
	 * Hints:
	 * 1. execute sqlquery with options ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE so that you can update rows.
	 * 2. for each row, process each assessment (using a for loop with variable i)
	 * 		- get the file from multipart: javax.servlet.http.Part file = request.getPart(at_no[i]) 
	 * 			(at_no[i] is the same as defined in upload_assessment.jsp)
	 * 		- if file is not null, get its inputstream using getInputStream() method and get its file name 
	 * 			using the provided getFileName(final javax.servlet.http.Part part) method
	 * 		- save the file extension as the assessment type
	 * 		- if the file's inputstream has something to read (available() >0) and the assessment status is not null 
	 * 			(based on the hidden at_no[i] variable from upload_assessment.jsp), set the assessment status in the database
	 * 			and save the inputstream into the database as an assessment (at_label[i]) uploaded by nlic 
	 * 			or as a reviewed assessment (reviewer1_at[i], reviewer2_at[i], or reviewer3_at[i]) uploaded by a pr
	 * 		- update the row and process the next row.
	 * 
	 * Sprint 4 needs to: (1) set each assessment status from "reviewerX" to "reviewed" after it
	 * 		has been reviewed by all assigned reviewers, (2) return an appropriate "uploadState" 
	 * Hints: for (1), if an assessment status starts with "reviewer", you need to cross check "reviewerX" and "reviewerX_at[i]". 
	 * 			The logic is (getString("reviewerX")==null || getBlob(reviewerX_at[i]) != null). If reviewerX does not exist (first one is true),
	 * 			the second one will not be evaluated. Otherwise, the second one must be true (i.e., reviewed assessment uploaded). When all
	 * 			expected reviewers have uploaded their reviewed assessments, the status is changed to "reviewed".
	 * 		  for (2), use the "states" variable to record relevant statuses and then use string searching to determine the correct "uploadState"
	 */
	String[] at_no = {Integer.toString(row) + "_1", Integer.toString(row) + "_2", Integer.toString(row) + "_3"};
	public int uploadAssessments(HttpServletRequest request, String sqlquery) {				
		int uploadState = -1; // -1: upload unsuccessful, 0: only uploaded by nlic, 1: only revised by nlic,
		// 2: mix of uploaded and revised by nlic, 3: reviewed by pr
		String states = ""; // If it contains "reviewed" (reviewed by all reviewers) or "reviewer" (not
		String test = "";					// reviewed by all reviewers),
		// uploadState should return 3. If it contains both "uploaded" and "revised", it
		// should return 2.
		// If it contains "revised", it should return 1. If it contains "uploaded", it
		// should return 0.

		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			Connection conn = DriverManager.getConnection(Configuration.dbConnectionURL);
			PreparedStatement ps = conn.prepareStatement(sqlquery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = ps.executeQuery();
			String name;
			String extension;
			String rolefix;

			while (rs.next()) {
				for (int i = 0; i < 3; i++) {
					javax.servlet.http.Part file = request.getPart(at_no[i]);

					if (file != null) {
						InputStream input = file.getInputStream();
						name = getFileName(file);
						
						rolefix = request.getParameter("role");
						rs.updateRow();
						if (input.available() > 0) {
							if (rolefix.equals("pr")) {
								if (request.getParameter(at_no[i]).equals("reviewer1")) {
									rs.updateBlob(reviewer1_at[i], input);
									rs.updateString(at_status[i], request.getParameter(at_no[i]));
									states = "reviewer";
									rs.updateRow();
									
									
								}
								if (request.getParameter(at_no[i]).equals("reviewer2")) {
									rs.updateBlob(reviewer2_at[i], input);
									rs.updateString(at_status[i], request.getParameter(at_no[i]));
									states = "reviewer";
									rs.updateRow();
									
								}
								if (request.getParameter(at_no[i]).equals("reviewer3")) {
									rs.updateBlob(reviewer3_at[i], input);
									rs.updateString(at_status[i], request.getParameter(at_no[i]));
									states = "reviewer";
									rs.updateRow();
									
								}
								if ((rs.getString("reviewer1") == null || rs.getBlob(reviewer1_at[i]) != null)
										&& (rs.getString("reviewer2") == null || rs.getBlob(reviewer2_at[i]) != null)
										&& (rs.getString("reviewer3") == null || rs.getBlob(reviewer3_at[i]) != null)) {
									rs.updateString(at_status[i], "reviewed");
									states = "reviewed";
									
								}

							}
							if (rs.getString(at_status[i]) == null) {
								rs.updateBlob(at_label[i], input);
								rs.updateString(at_status[i], request.getParameter(at_no[i]));
								extension = name.substring(name.lastIndexOf(".") + 1);
								rs.updateString(at_type[i], extension);
								rs.updateRow();
								states = "uploaded";
								
							} else if (rs.getString(at_status[i]).equals("lt_returned")) {
								rs.updateBlob(at_label[i], input);
								rs.updateString(at_status[i], request.getParameter(at_no[i]));
								rs.updateRow();
								
								
							} else if (rs.getString(at_status[i]).equals("dh_returned")) {
								rs.updateBlob(at_label[i], input);
								rs.updateString(at_status[i], request.getParameter(at_no[i]));
								rs.updateRow();
								
								
							} else if(rs.getString(at_status[i]).equals("reviewed")) {
								rs.updateBlob(at_label[i], input);
								rs.updateString(at_status[i], request.getParameter(at_no[i]));
								System.out.println(request.getParameter(at_no[i]));
								rs.updateRow();
								
							}else if(rs.getString(at_status[i]).equals("revised")) {
								rs.updateBlob(at_label[i], input);
								System.out.println(request.getParameter(at_no[i]));
								rs.updateRow();
							}
						}

					}
					
					
					rs.updateRow();
					
				}
				test = rs.getString(at_status[0]) +" "+ rs.getString(at_status[1]) +" "+ rs.getString(at_status[2]);
				
			}
			System.out.println(test);
			if(test.contains("uploaded") && !(test.contains("revised"))) {
				uploadState=0;
			}
			else if(test.contains("revised") && !(test.contains("uploaded"))) {
				uploadState=1;
			}
			else if(test.contains("revised") && test.contains("uploaded")) {
				uploadState=2;
			}
			else if(test.contains("reviewer1") || test.contains("reviewer2") || test.contains("reviewer3") || test.contains("reviewed")) {
				uploadState=3;
			} 
		System.out.println(uploadState);

			rs.close();
			ps.close();
			conn.close();
			
			
		} catch (Exception e) {

			e.printStackTrace();
		}
		return uploadState;
	}
	
	// download assessments from database to the web server and make it accessible via urls
	/* Sprint 5 is to complete this method
	 * Hints:
	 * 1. execute sqlquery to retrieve relevant units
	 * 2. for each row, process each assessment (using a for loop with variable i)
	 * 		- reconstruct the full file name at_id+type (at_id as in download_assessments.jsp)
	 * 		- download the assessment to file by using the method downloadToFile(HttpServletRequest request, String filename, Blob b)
	 * 		- download all reviewed assessments using the same method
	 */
	public void downloadAssessment(HttpServletRequest request, String sqlquery) {
		try {Class.forName("com.mysql.cj.jdbc.Driver");
		Connection conn = DriverManager.getConnection(Configuration.dbConnectionURL);
		PreparedStatement ps = conn.prepareStatement(sqlquery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
				if(request.getParameter("role").equals("lt") 
				|| request.getParameter("role").equals("dh") 
				|| request.getParameter("role").equals("pr")) {
					for(int i = 0; i <3 ;i++) {
						if(at_label[i] != null && at_label[i].equals("at1")) {
							String at_id = rs.getString("code") + "_" + at_label[i];
							String filename = at_id+rs.getString(at_type[i]);
							System.out.println(filename);
							downloadToFile(request,filename,rs.getBlob(at_label[i]));
					}else if(at_label[i] !=null && at_label[i].equals("at2")) {
						String at_id = rs.getString("code") + "_" + at_label[i];
						String filename = at_id+rs.getString(at_type[i]);
						System.out.println(filename);
						downloadToFile(request,filename,rs.getBlob(at_label[i]));
					}else if(at_label[i] != null && at_label[i].equals("at3")) {
						String at_id = rs.getString("code") + "_" + at_label[i];
						String filename = at_id+rs.getString(at_type[i]);
						System.out.println(filename);
						downloadToFile(request,filename,rs.getBlob(at_label[i]));
					}
				}
					
			}
			if(request.getParameter("role").equals("nlic")) {
				for(int i = 0; i < 3; i++) {
					if(reviewer1_at[i] != null && (at_label[i] + "_r1").equals("at1_r1")) {
						String at_id10 = rs.getString("code") + "_" + at_label[i] ; 
						String filename10 = at_id10+"_r1"+rs.getString(at_type[i]);
						downloadToFile(request,filename10,rs.getBlob(reviewer1_at[i]));
					}
					if(reviewer1_at[i] != null && (at_label[i] + "_r1").equals("at2_r1")){
						String at_id2 = rs.getString("code") + "_" + at_label[i];
						String filename2 = at_id2+"_r1"+rs.getString(at_type[i]);
						downloadToFile(request,filename2,rs.getBlob(reviewer1_at[i]));
					}
					if(reviewer1_at[i] != null && (at_label[i] + "_r1").equals("at3_r1")) {
						String at_id3 = rs.getString("code") + "_" + at_label[i];
						String filename3 = at_id3+"_r1"+rs.getString(at_type[i]);
						downloadToFile(request,filename3,rs.getBlob(reviewer1_at[i]));
					}
					if(reviewer2_at[i] !=null && (at_label[i] + "_r2").equals("at1_r2")){
						String at_id4 = rs.getString("code") + "_" + at_label[i];
						String filename4 = at_id4+"_r2"+rs.getString(at_type[i]);
						downloadToFile(request,filename4,rs.getBlob(reviewer2_at[i]));
					}
					if(reviewer2_at[i] !=null && (at_label[i] + "_r2").equals("at2_r2")){
						String at_id5 = rs.getString("code") + "_" + at_label[i];
						String filename5 = at_id5+"_r2"+rs.getString(at_type[i]);
						downloadToFile(request,filename5,rs.getBlob(reviewer2_at[i]));
					}
					if(reviewer2_at[i] !=null && (at_label[i] + "_r2").equals("at3_r2")) {
						String at_id6 = rs.getString("code") + "_" + at_label[i];
						String filename6 = at_id6+"_r2"+rs.getString(at_type[i]);
						downloadToFile(request,filename6,rs.getBlob(reviewer2_at[i]));
					}
					if(reviewer3_at[i] !=null && (at_label[i] + "_r3").equals("at1_r3")){
						String at_id7 = rs.getString("code") + "_" + at_label[i];
						String filename7 = at_id7+"_r3"+rs.getString(at_type[i]);
						downloadToFile(request,filename7,rs.getBlob(reviewer3_at[i]));
					}
					if(reviewer3_at[i] !=null && (at_label[i] + "_r3").equals("at2_r3")) {
						String at_id8 = rs.getString("code") + "_" + at_label[i];
						String filename8 = at_id8+"_r3"+rs.getString(at_type[i]);
						downloadToFile(request,filename8,rs.getBlob(reviewer3_at[i]));
					}
					if(reviewer3_at[i] !=null && (at_label[i] + "_r3").equals("at3_r3")) {
						String at_id9 = rs.getString("code") + "_" + at_label[i];
						String filename9 = at_id9+"_r3"+rs.getString(at_type[i]);
						downloadToFile(request,filename9,rs.getBlob(reviewer3_at[i]));
					}
					}
				}
			
			}
			
		
			
		}
		catch (Exception e) {

			e.printStackTrace();
		}
	}
	
	// save blob (b) from database to a file (filename)
	private void downloadToFile(HttpServletRequest request, String filename, Blob b)
			throws SQLException, FileNotFoundException, IOException {
		// convert blob into a byte array
		byte[] bdata = b.getBytes(1, (int) b.length());
		// location of the file
		final File f = new File(request.getServletContext().getRealPath(".") + "/" + filename);
		System.out.println(f);
		// write the byte array to the file
		OutputStream output = new FileOutputStream(f);				     	
		output.write(bdata); 
		output.flush();
		output.close();
	}
	
	// Return the name of the uploaded file from a multipart body in a form
	// example:
	// Content-Disposition: form-data; name="fieldName"
	// Content-Disposition: form-data; name="fieldName"; filename="filename.jpg"
	private String getFileName(final javax.servlet.http.Part part) {
	    for (String content : part.getHeader("content-disposition").split(";")) {
	        if (content.trim().startsWith("filename")) {
	            return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
	        }
	    }
	    return null;
	}
	
	// This method checks which reviewer the login user is
	public String whichReviewer(HttpServletRequest request, HttpSession session, ResultSet resultSet) throws Exception {
		String reviewer = null;

		String role = request.getParameter("role");
		String user = session.getAttribute("login").toString();

		if (resultSet != null && role.equals("pr")) {
			if (resultSet.getString("reviewer1") != null && resultSet.getString("reviewer1").equals(user)) {
				reviewer = "reviewer1";
			}
			else if (resultSet.getString("reviewer2") != null && resultSet.getString("reviewer2").equals(user)) {
				reviewer = "reviewer2";
			}
			else if (resultSet.getString("reviewer3") != null && resultSet.getString("reviewer3").equals(user)) {
				reviewer = "reviewer3";
			}
		}
		return reviewer;
		}
}