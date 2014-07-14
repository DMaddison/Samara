package mesquite.samara.InterpretDELTA;

import mesquite.categ.lib.*;
import mesquite.cont.lib.*;
import mesquite.meristic.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import java.io.*;


/* TODO: 
 * 
 * support full state ranges/descriptions

*/

public class InterpretDELTA extends FileInterpreterI {


	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}

	/*.................................................................................................................*/
	public boolean canExportEver() {  
		return false;
	}
	/*.................................................................................................................*/
	public boolean canExportProject(MesquiteProject project) {  
		return false;
	}
	/*.................................................................................................................*/
	public boolean canExportData(Class dataClass) {  
		return false;
	}
	/*.................................................................................................................*/
	public boolean canImport(Class dataClass){
		return true;
	}
	/*.................................................................................................................*/
	public boolean canImport() {  
		return true;
	}

	public boolean exportFile(MesquiteFile file, String arguments) {
		return false;

	}


	public String getName() {
		return "DELTA Directives File";
	}

	public String getExplanation() {
		return "This file should contain DELTA INPUT FILE directives for the specs, chars, and items files; e.g., \n*INPUT FILE specs \n*INPUT FILE chars \n*INPUT FILE items";
	}


	boolean someContinuous = false;
	boolean someMeristic=false;
	Parser subParser = new Parser();
	int[] charTypes = null;
	long[] implicitCateg = null;
	double[] implicitContinuous = null;
	static int UM = 0;
	static int OM = 1;
	static int IN = 2;
	static int RN = 3;
	static int TE = 4;

	/*.................................................................................................................*/
	public String getCommentContents(MesquiteFile file, Parser parser, MesquiteBoolean abort){
		int closeCommentDebt = 1;
		String s = parser.getRemaining();   // can't use this - need to go through all lines
		int numCloseComments = StringUtil.getNumMatchingChars(s, '>');
		int numOpenComments;
		if (numCloseComments>0) { // there is a close comment in the first line
			if (StringUtil.getNumMatchingChars(s, '<')<=0)  // not a nested comment, so we can just get the end of the comment from this single line
				return parser.getRemainingUntilChar('>', false);   // 
			else if (StringUtil.getNumMatchingChars(s, '<')+1<=numCloseComments ){ // is nested, but ends on this line, as there at least as many close comments than needed
				s = "";
				numOpenComments = 0;
				for (int i=1; i<=numCloseComments; i++) {
					String part = parser.getRemainingUntilChar('>', false); 
					numOpenComments = StringUtil.getNumMatchingChars(part, '<');
					s+= part;   
					if (numOpenComments+1==i)
						return s;
				}
				return s;
			} else {   // close comments are on other lines.
				closeCommentDebt = StringUtil.getNumMatchingChars(s, '<')+1 - numCloseComments; //
			}
		}
		String line = file.readLine();  // if we are here, it means the comment hasn't been closed yet.
		line = StringUtil.stripLeadingWhitespace(line);
		parser.setString(line);
		while ((line !=null || !file.atEOF()) && !abort.getValue()) {
			numCloseComments = StringUtil.getNumMatchingChars(line, '>');
			numOpenComments = StringUtil.getNumMatchingChars(line, '<');
			if (file.getFileAborted()) {
				abort.setValue(true);
			}
			parser.setString(line); //sets the string to be used by the parser to "line" and sets the pos to 0
			s = StringUtil.stripTrailingWhitespace(s) + " ";
			if (numCloseComments>0) {
				if (StringUtil.getNumMatchingChars(s, '<')<=0)  // not a nested comment
					return s+parser.getRemainingUntilChar('>', false);   // 
				else if (StringUtil.getNumMatchingChars(s, '<')+closeCommentDebt<=numCloseComments ){ // is nested, but ends on this line
					numOpenComments = 0;
					for (int i=1; i<=numCloseComments; i++) {
						String part = parser.getRemainingUntilChar('>', false); 
						numOpenComments = StringUtil.getNumMatchingChars(part, '<');
						s+= part;   
						if (numOpenComments+closeCommentDebt==i)
							return s;
					}
					return s;
				}
			} else s+= line;
			long oldPos = file.getFilePosition();
			line = file.readLine();
			line = StringUtil.stripLeadingWhitespace(line);
			if (line.startsWith("#")) {
				file.goToFilePosition(oldPos);
				return s;
			}
		}
		return null;
	}

	/*.................................................................................................................*/
	public void readItemsFile(MesquiteFile file, CategoricalData data, ContinuousData continuousData, MeristicData meristicData, Taxa taxa, String line, MesquiteBoolean abort) {
		if (data==null || taxa==null)
			return;
		int ic=MesquiteInteger.unassigned;
		int it = -1;
		MesquiteInteger icStart = new MesquiteInteger();
		MesquiteInteger icEnd = new MesquiteInteger();
		boolean readingTaxon = false;
		String punct = parser.getPunctuationString();

		while ((line !=null || !file.atEOF()) && !abort.getValue()) {
			if (file.getFileAborted()) {
				abort.setValue(true);
			}
			line = StringUtil.stripLeadingWhitespace(line);
			parser.setString(line); //sets the string to be used by the parser to "line" and sets the pos to 0
			if (line.startsWith("#")) {  
				line=line.substring(1);
				line = StringUtil.stripLeadingWhitespace(line);
				parser.setString(line);
				String taxonName = parser.getRemainingUntilChar('/', true);
				it++;
				taxa.setTaxonName(it,taxonName);
				readingTaxon=true;

			} else if (line.startsWith("*")) {  
				readingTaxon=false;
			}

			String range;
			String value;
			String comment="";

			if (readingTaxon)
				while (!parser.atEnd()) {
					range = parser.getRemainingUntilChars(",<", false);
					readCharRange(icStart,icEnd,range);

					String s = parser.getNextDarkChars(1);
					if (s.equals(",")) {
						parser.setPunctuationString("<");
						value = parser.getNextToken();
					}
					else if (s.equals("<")) {
						value ="";
						comment = getCommentContents( file,  parser,  abort);
					}
					else value = null;
					if (value!=null && charTypes!=null)
						for (int i=icStart.getValue(); i<data.getNumChars() && i<=icEnd.getValue(); i++) {
							if (i>=0)
								if (charTypes[i] == OM || charTypes[i] == UM) {
									if (value.equalsIgnoreCase("V"))
										data.setToUnassigned(i, it);   
									else if (value.equalsIgnoreCase("U"))
										data.setToUnassigned(i, it);   
									else if (value.equals("-"))
										data.setToInapplicable(i, it);   
									else {
										int stateInt = MesquiteInteger.fromString(value)-1;
										data.setState(i, it, CategoricalState.makeSet(stateInt));   
									}

								} else if (charTypes[i]==IN) {
									MesquiteInteger first = new MesquiteInteger();
									MesquiteInteger last = new MesquiteInteger();
									readStateRange(first, last, value);
									//int stateInt = MesquiteInteger.fromString(value);
									//meristicData.addItem();
									meristicData.setState(i, it, 0, first.getValue());   
								} else if (charTypes[i]==RN) {
									double stateDouble = MesquiteDouble.fromString(value);
									continuousData.setState(i, it, 0, stateDouble);   
								} else if (charTypes[i]==TE) {
									String current = taxa.getAnnotation(it);
									if (!StringUtil.blank(current))
										value = current + "  " + value;
									taxa.setAnnotation(it, value);
								}
						}
				}

			line = file.readLine();
		}
	}


	/*.................................................................................................................*/
	public void readCharRange(MesquiteInteger icStart, MesquiteInteger icEnd, String range) {
		if (range.indexOf("-")>=0) {
			range = StringUtil.replace(range, '-', ' ');
			subParser.setString(range);
			icStart.setValue(MesquiteInteger.fromString(subParser.getFirstToken()));
			icEnd.setValue(MesquiteInteger.fromString(subParser.getNextToken()));
		} else {
			icStart.setValue(MesquiteInteger.fromString(range));
			icEnd.setValue(MesquiteInteger.fromString(range));
		}
		icStart.decrement();
		icEnd.decrement();

	}
	/*.................................................................................................................*/
	public void readStateRange(MesquiteInteger first, MesquiteInteger last, String range) {
		if (range.indexOf("-")>=0) {
			range = StringUtil.replace(range, '-', ' ');
			subParser.setString(range);
			first.setValue(MesquiteInteger.fromString(subParser.getFirstToken()));
			last.setValue(MesquiteInteger.fromString(subParser.getNextToken()));
		} else {
			first.setValue(MesquiteInteger.fromString(range));
			last.setValue(MesquiteInteger.fromString(range));
		}
	}
	/*.................................................................................................................*/
	public void readRange(MesquiteInteger icStart, MesquiteInteger icEnd, String range) {
		if (range.indexOf("-")>=0) {
			range = StringUtil.replace(range, '-', ' ');
			subParser.setString(range);
			icStart.setValue(MesquiteInteger.fromString(subParser.getFirstToken()));
			icEnd.setValue(MesquiteInteger.fromString(subParser.getNextToken()));
		} else {
			icStart.setValue(MesquiteInteger.fromString(range));
			icEnd.setValue(MesquiteInteger.fromString(range));
		}
		icStart.decrement();
		icEnd.decrement();

	}

	/*.................................................................................................................*/
	public void readSpecsFile(MesquiteFile file, CategoricalData data, ContinuousData continuousData, MeristicData meristicData, Taxa taxa, String line, MesquiteBoolean abort) {
		if (data==null || taxa==null)
			return;
		int ic=MesquiteInteger.unassigned;
		int numChars = MesquiteInteger.unassigned;
		int numTaxa = MesquiteInteger.unassigned;
		MesquiteInteger icStart = new MesquiteInteger();
		MesquiteInteger icEnd = new MesquiteInteger();


		while ((line !=null || !file.atEOF()) && !abort.getValue()) {
			line = StringUtil.stripLeadingWhitespace(line);
			parser.setString(line); //sets the string to be used by the parser to "line" and sets the pos to 0
			if (line.startsWith("*")) {  
				line=line.substring(1);
				if (line.startsWith("NUMBER OF CHARACTERS")) {
					line = line.replaceFirst("NUMBER OF CHARACTERS", "");
					line = StringUtil.stripBoundingWhitespace(line);
					numChars = MesquiteInteger.fromString(line);
					data.addCharacters(data.getNumChars()-1, numChars, false);   // add a character
					charTypes = new int[numChars];
					for (int i=0; i<numChars; i++)
						charTypes[i]=UM;
				} 
				else if (line.startsWith("MAXIMUM NUMBER OF ITEMS")) {
					line = line.replaceFirst("MAXIMUM NUMBER OF ITEMS", "");
					line = StringUtil.stripBoundingWhitespace(line);
					numTaxa = MesquiteInteger.fromString(line);
					taxa.addTaxa(0, numTaxa, true);
				} 
				else if (line.startsWith("CHARACTER TYPES")) {
					/* format is    c1,t1 c2,t2 ...ci,ti ... 
						where ci is a character number or range of numbers, and ti is one of the following character types.
						unordered multistate (UM), ordered multistate (OM), integer numeric (IN), real numeric  (RN), and text (TE).
						default is UM
					 */
					line = line.replaceFirst("CHARACTER TYPES", "");
					line = StringUtil.stripBoundingWhitespace(line);

					while ((line !=null || !file.atEOF()) && !abort.getValue() && line.indexOf('*')<0) { 
						if (file.getFileAborted())
							abort.setValue(true);
						line = StringUtil.stripLeadingWhitespace(line);
						parser.setString(line);

						String range;
						String cType;

						while (!parser.atEnd()) {
							range = parser.getRemainingUntilChar(',', true);
							readCharRange(icStart,icEnd,range);
							cType = parser.getNextToken();
							int ct = UM;
							if (cType!=null) {
								if (cType.equals("OM"))
									ct = OM;
								else if (cType.equals("IN")) {
									ct = IN;
									someMeristic = true;
								}
								else if (cType.equals("RN")) {
									ct = RN;
									someContinuous = true;
								}
								else if (cType.equals("TE"))
									ct = TE;
								if (charTypes!=null)
									for (int i=icStart.getValue(); i<numChars && i<=icEnd.getValue(); i++) {
										if (i>=0)
											charTypes[i] = ct;
									}
							} 
						}

						line = file.readLine();
					}

					if (someContinuous)
						continuousData.addCharacters(continuousData.getNumChars()-1, numChars, false);   // add a character
					if (someMeristic)
						meristicData.addCharacters(meristicData.getNumChars()-1, numChars, false);   // add a character

				} 
				else if (line.startsWith("IMPLICIT VALUES")) {
					line = line.replaceFirst("IMPLICIT VALUES", "");
					line = StringUtil.stripBoundingWhitespace(line);
					implicitCateg = new long[numChars];
					implicitContinuous = new double[numChars];


					while ((line !=null || !file.atEOF()) && !abort.getValue() && line.indexOf('*')<0) { 
						if (file.getFileAborted())
							abort.setValue(true);
						line = StringUtil.stripLeadingWhitespace(line);
						parser.setString(line);

						String range;
						String value;

						while (!parser.atEnd()) {
							range = parser.getRemainingUntilChar(',', true);
							readCharRange(icStart,icEnd,range);
							value = parser.getNextToken();
							if (value!=null && charTypes!=null)
								for (int i=icStart.getValue(); i<numChars && i<=icEnd.getValue(); i++) {
									if (i>=0)
										if (charTypes[i] == OM || charTypes[i] == UM) {

										} else if (charTypes[i]==RN) {
										}
								}
						} 
						line = file.readLine();
					}
				}

				else if (line.startsWith("DEPENDENT CHARACTERS")) {
					line = line.replaceFirst("DEPENDENT CHARACTERS", "");
					line = StringUtil.stripBoundingWhitespace(line);
				} 
			}

			if (line!=null && line.indexOf('*')<0)  // don't read another line if there is a * in this line
				line = file.readLine();
			if (file.getFileAborted()) {
				abort.setValue(true);
			}
		}
	}

	/*.................................................................................................................*/
	public void readCharsFile(MesquiteFile file, CategoricalData data, ContinuousData continuousData, MeristicData meristicData, String line, MesquiteBoolean abort) {
		boolean readingStates = false;
		int ic=MesquiteInteger.unassigned;


		while ((line !=null || !file.atEOF()) && !abort.getValue()) {
			line = StringUtil.stripLeadingWhitespace(line);
			parser.setString(line); //sets the string to be used by the parser to "line" and sets the pos to 0
			if (line.startsWith("*")) {  // new character
				readingStates = false;
			}
			else if (line.startsWith("#")) {  // new character
				readingStates=true;
				String num = parser.getFirstToken();  // #
				if (num.equals("#"))
					num = parser.getNextToken();
				num = StringUtil.replace(num, '#', ' ');
				num = StringUtil.replace(num, '.', ' ');
				num = StringUtil.stripBoundingWhitespace(num);
				ic = MesquiteInteger.fromString(num)-1;
				String charName = parser.getRemainingUntilChar('/');
				while ((line !=null || !file.atEOF())  && !abort.getValue() && line.indexOf('/')<0) {  // pulling off character name
					//file.readLine(sb);
					// line = sb.toString();		
					line = file.readLine();
					if (file.getFileAborted())
						abort.setValue(true);
					line = StringUtil.stripLeadingWhitespace(line);
					parser.setString(line);
					charName += " " + parser.getRemainingUntilChar('/');
				}
				if (ic<0 || !MesquiteInteger.isCombinable(ic)) {
					data.addCharacters(data.getNumChars()-1, 1, false);   // add a character
					ic = data.getNumChars()-1;
				} else if (ic>=data.getNumChars()) {
					data.addCharacters(data.getNumChars()-1, ic-data.getNumChars()+1, false);   // add a character
					ic = data.getNumChars()-1;
				}
				if (charTypes[ic]==OM || charTypes[ic]==UM)
					data.setCharacterName(ic, charName);
				else if (charTypes[ic]==RN)
					continuousData.setCharacterName(ic, charName);
				else if (charTypes[ic]==IN)
					meristicData.setCharacterName(ic, charName);
			} else if (readingStates && !StringUtil.blank(line)) {

				String num = parser.getFirstToken();  // #
				num = StringUtil.replace(num, '.', ' ');
				num = StringUtil.stripBoundingWhitespace(num);
				int stateNum = MesquiteInteger.fromString(num)-1;
				String stateName = parser.getRemainingUntilChar('/');
				while ((line !=null || !file.atEOF())  && !abort.getValue() && line.indexOf('/')<0) {  // pulling off character name
					//file.readLine(sb);
					// line = sb.toString();		
					line = file.readLine();
					if (file.getFileAborted())
						abort.setValue(true);
					line = StringUtil.stripLeadingWhitespace(line);
					parser.setString(line);
					stateName += " " + parser.getRemainingUntilChar('/');
				}
				if (MesquiteInteger.isCombinable(ic))
					data.setStateName(ic, stateNum, stateName);
			}

			line = file.readLine();
			if (file.getFileAborted()) {
				abort.setValue(true);
			}
		}

		if (charTypes==null) {
			charTypes = new int[data.getNumChars()];
			for (int i=0; i<data.getNumChars(); i++)
				charTypes[i]=UM;
		}


	}


	/*.................................................................................................................*/
	public String findFileType(String filePath) {
		MesquiteFile file = new MesquiteFile();
		file.setPath(filePath);
		if (file.openReading(false)) {

			StringBuffer sb = new StringBuffer(1000);
			file.readLine(sb);
			String line = sb.toString();

			while ((line !=null || !file.atEOF())) {
				line = StringUtil.stripLeadingWhitespace(line);
				parser.setString(line); //sets the string to be used by the parser to "line" and sets the pos to 0
				if (line.startsWith("*")) {  
					line=line.substring(1);
					if (line.startsWith("NUMBER OF CHARACTERS")) {
						return "specs";
					} 
					else if (line.startsWith("MAXIMUM NUMBER OF ITEMS")) {
						return "specs";
					} 
					else if (line.startsWith("CHARACTER TYPES")) {
						return "specs";
					} 
					else if (line.startsWith("CHARACTER LIST")) {
						return "chars";
					} 
					else if (line.startsWith("ITEM DESCRIPTIONS")) {
						return "items";
					} 
				}

				line = file.readLine();

			}
			file.closeReading();
		}
		return null;
	}
	/*.................................................................................................................*/
	public void readFile(MesquiteProject mf, MesquiteFile file, String arguments) {
		incrementMenuResetSuppression();
		ProgressIndicator progIndicator = new ProgressIndicator(mf,"Importing DELTA files "+ file.getName(), file.existingLength());
		String charsFile = null;
		String specsFile = null;
		String itemsFile = null;
		someContinuous = false;
		someMeristic = false;

		if (file.openReading()) {


			StringBuffer sb = new StringBuffer(1000);
			file.readLine(sb);
			String line = sb.toString();

			boolean foundDelta = false;

			while (line !=null || !file.atEOF()) {
				line = StringUtil.stripLeadingWhitespace(line);
				if (line.startsWith("*INPUT FILE")) {
					line=line.substring(1);
					line = line.replaceFirst("INPUT FILE", "");
					parser.setString(line);
					String fileName = parser.getNextToken();
					if (!StringUtil.blank(fileName)) {
						fileName = file.getDirectoryName() + fileName;
						String fileType = findFileType(fileName);
						if (fileType!=null)
							if (fileType.equalsIgnoreCase("chars"))
								charsFile = fileName;
							else if (fileType.equalsIgnoreCase("items"))
								itemsFile = fileName;
							else if (fileType.equalsIgnoreCase("specs"))
								specsFile = fileName;
					}
				}
				line = file.readLine();
			}

		}

		if (specsFile!=null || charsFile!=null) {
			TaxaManager taxaTask = (TaxaManager)findElementManager(Taxa.class);
			CharactersManager charTask = (CharactersManager)findElementManager(CharacterData.class);
			Taxa taxa = taxaTask.makeNewTaxa(getProject().getTaxas().getUniqueName("Untitled Block of Taxa"), 0, false);
			taxa.addToFile(file, getProject(), taxaTask);
			int numTaxa = 0;
			MesquiteBoolean abort = new MesquiteBoolean(false);

			progIndicator.start();
			CategoricalData data=null;
			ContinuousData continuousData = null;
			MeristicData meristicData = null;
			boolean wassave = false;

			if (specsFile!=null) {
				MesquiteFile oneFile = new MesquiteFile();
				oneFile.setPath(specsFile);
				oneFile.linkProgressIndicator(progIndicator);
				if (oneFile.openReading()) {

					data = (CategoricalData)charTask.newCharacterData(taxa, 0, CategoricalData.DATATYPENAME);
					continuousData = (ContinuousData)charTask.newCharacterData(taxa, 0, ContinuousData.DATATYPENAME);
					meristicData = (MeristicData)charTask.newCharacterData(taxa, 0, MeristicData.DATATYPENAME);

					data.addToFile(file, getProject(), null);
					data.setName("Categorical Matrix from DELTA");
					wassave = data.saveChangeHistory;
					data.saveChangeHistory = false;

					StringBuffer sb = new StringBuffer(1000);
					oneFile.readLine(sb);
					String line = sb.toString();

					readSpecsFile(oneFile,  data,  continuousData, meristicData, taxa, line,  abort);

					if (someContinuous) {
						continuousData.addToFile(file, getProject(), null);
						continuousData.setName("Continuous Matrix from DELTA");
					}
					if (someMeristic) {
						meristicData.addToFile(file, getProject(), null);
						meristicData.setName("Meristic Matrix from DELTA");
					}


				}


			}

			if (charsFile!=null) {
				MesquiteFile oneFile = new MesquiteFile();
				oneFile.setPath(charsFile);
				oneFile.linkProgressIndicator(progIndicator);
				if (oneFile.openReading()) {

					if (data==null) { 
						data = (CategoricalData)charTask.newCharacterData(taxa, 0, CategoricalData.DATATYPENAME);
						data.addToFile(file, getProject(), null);
						wassave = data.saveChangeHistory;
						data.saveChangeHistory = false;
					}

					StringBuffer sb = new StringBuffer(1000);
					oneFile.readLine(sb);
					String line = sb.toString();

					if (taxa.getNumTaxa()<=0)
						taxa.addTaxa(numTaxa-1, 1, true);

					readCharsFile(oneFile,  data,  continuousData, meristicData, line,  abort);

				}
			}

			if (itemsFile!=null) {
				MesquiteFile oneFile = new MesquiteFile();
				oneFile.setPath(itemsFile);
				oneFile.linkProgressIndicator(progIndicator);
				if (oneFile.openReading()) {


					StringBuffer sb = new StringBuffer(1000);
					oneFile.readLine(sb);
					String line = sb.toString();

					readItemsFile(oneFile,  data,  continuousData, meristicData, taxa, line,  abort);

				}
			}

			if (data!=null) {
				data.removeCharactersThatAreEntirelyUnassigned(false);
				data.saveChangeHistory = wassave;
				data.resetChangedSinceSave();
			}
			if (someContinuous && continuousData!=null){
				continuousData.removeCharactersThatAreEntirelyUnassigned(false);
				continuousData.saveChangeHistory = wassave;
				continuousData.resetChangedSinceSave();
			}
			if (someMeristic && meristicData!=null){
				meristicData.removeCharactersThatAreEntirelyUnassigned(false);
				meristicData.saveChangeHistory = wassave;
				meristicData.resetChangedSinceSave();
			}

			finishImport(progIndicator, file, abort.getValue());

		}
		decrementMenuResetSuppression();
	}



}
