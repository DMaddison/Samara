package mesquite.samara.aSamaraIntro;

import mesquite.lib.duties.PackageIntro;

public class aSamaraIntro extends PackageIntro {

		/*.................................................................................................................*/
		public boolean startJob(String arguments, Object condition, boolean hiredByName) {
	 		return true;
	  	 }
	  	 public Class getDutyClass(){
	  	 	return aSamaraIntro.class;
	  	 }
	 	/*.................................................................................................................*/
		 public String getExplanation() {
		return "Samara is a package of Mesquite modules providing tools for interactive key software. "
			+ "A \"samara\" is a formal name for the winged fruit of maple trees, genus Acer; these fruit are also known as \"keys\"." ;
		 }
	   
		/*.................................................................................................................*/
	    	 public String getName() {
			return "Samara Package";
	   	 }
		/*.................................................................................................................*/
		/** Returns the name of the package of modules (e.g., "Basic Mesquite Package", "Rhetenor")*/
	 	public String getPackageName(){
	 		return "Samara Package";
	 	}
		/*.................................................................................................................*/
		/** Returns citation for a package of modules*/
	 	public String getPackageCitation(){
	 		return "Maddison, D.R.  2008.  Samara.  A package of modules for Mesquite. Version 0.1.";
	 	}
		/*.................................................................................................................*/
		/** Returns version for a package of modules*/
		public String getPackageVersion(){
			return "0.1";
		}
		/*.................................................................................................................*/
		/** Returns version for a package of modules as an integer*/
		public int getPackageVersionInt(){
			return 10;
		}
		/*.................................................................................................................*/
		/** returns the URL of the notices file for this module so that it can phone home and check for messages */
		public String  getHomePhoneNumber(){ 
			return "http://mesquiteproject.org/packages/samara/notices.xml";
		}

		public String getPackageDateReleased(){
			return "15 May 2008";
		}
		/*.................................................................................................................*/
		/** Returns whether there is a splash banner*/
		public boolean hasSplash(){
	 		return true; 
		}
		/*.................................................................................................................*/
		public int getVersionOfFirstRelease(){
			return NEXTRELEASE;  
		}
	}
