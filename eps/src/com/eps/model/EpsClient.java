package com.eps.model;

/*
 * 4/4/2011 -- RE-WRITE WITHOUT OILP.exe
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import javax.servlet.http.HttpSession;

import com.ederbase.model.EbDynamic;
import com.ederbase.model.EbEnterprise;

/**
 * 
 * @author Rob Eder
 */
public class EpsClient {
	public String stVersion = "";
	private int iUserId = -1;
	private int nmPrivUser = 0;
	private String stAction = "";
	private String stTrace = "";
	private EbEnterprise ebEnt = null;
	private String stError = "";
	public EpsUserData epsUd = null;
	public HttpSession session = null;

	public EpsClient(EbEnterprise ebEnt, String stDbDyn) {
		this.ebEnt = ebEnt;
		if (this.ebEnt.ebDyn == null) {
			this.ebEnt.ebDyn = new EbDynamic(this.ebEnt);
		}
		this.ebEnt.ebDyn.assertDb(stDbDyn);
		if (this.ebEnt.ebUd != null && this.ebEnt.ebUd.request != null) {
			String stTemp = this.ebEnt.ebUd.request.getParameter("commtrace");
			if (stTemp != null && stTemp.length() > 0 && stTemp.equals("d")) {
				this.ebEnt.iDebugLevel = 1;
				this.ebEnt.dbCommon.setDebugLevel(1);
				this.ebEnt.dbDyn.setDebugLevel(1);
				this.ebEnt.dbEb.setDebugLevel(1);
				this.ebEnt.dbEnterprise.setDebugLevel(1);
			}
		}
		this.epsUd = new EpsUserData();

		try {
			InputStream is = this.getClass().getClassLoader()
					.getResourceAsStream("config.properties");
			Properties props = new Properties();
			props.load(is);
			stVersion = props.getProperty("epporaVersion");
		} catch (IOException e) {
		}
		this.epsUd.setEbEnt(ebEnt, this.stVersion);
		this.epsUd.epsEf.setEbEnt(ebEnt, this);
		this.epsUd.setUser(iUserId, 0);
	}

	public void setSession(HttpSession session) {
		this.session = session;
	}

	public String getEpsPage() {
		String stReturn = "";
		Map<String, String> recentURLs = (Map<String, String>) this.ebEnt.ebUd.request
				.getSession().getAttribute("recentURLs");
		if (recentURLs == null)
			recentURLs = new LinkedHashMap<String, String>();

		String stSql = "SELECT * FROM X25RefContent where nmRefPageId=1 order by nmUserId";
		try {

			iUserId = this.ebEnt.ebUd.getLoginId();
			nmPrivUser = this.ebEnt.ebUd.getLoginPersonFlags();

			stAction = this.ebEnt.ebUd.request.getParameter("stAction");
			if (stAction == null || stAction.length() <= 0)
				stAction = "home";

			String stPopupMessage = (String) this.ebEnt.ebUd.request
					.getSession().getAttribute("POPUP_MESSAGE");
			this.ebEnt.ebUd.request.getSession().removeAttribute(
					"POPUP_MESSAGE");
			if (stPopupMessage != null && stPopupMessage.length() > 0)
				ebEnt.ebUd.setPopupMessage(stPopupMessage);

			String stLookup = this.ebEnt.ebUd.request.getParameter("h");
			if (stLookup != null && stLookup.equals("n")) { // Load
															// Framework.Masterpage
															// for popup menus,
															// without TOP and
															// NAVIGATION
				stSql = "SELECT * FROM X25RefContent where nmContentId in (1,5,6)  order by nmUserId;";
			}
			ResultSet rs = this.ebEnt.dbEnterprise.ExecuteSql(stSql);
			if (rs != null) {
				rs.last();
				int iMax = rs.getRow();
				for (int iC = 1; iC <= iMax; iC++) {
					rs.absolute(iC);
					if ((rs.getInt("nmFlag") & 0x01) != 0) // Active
					{
						stReturn += processRules(rs.getInt("nmFlag"),
								rs.getString("stContent"));
					}
				}
			}
			this.ebEnt.ebDyn.setPopupMessage("alert",
					this.ebEnt.ebUd.getPopupMessage());
			stReturn += this.epsUd.getValitation();
			if (stTrace.length() > 0) {
				stReturn += "<hr>Trace:<br>" + stTrace
						+ "</td></tr></table><hr>";
			}

			String stPageTitle = this.epsUd.getPageTitle();
			stPageTitle = stPageTitle != null ? stPageTitle.trim() : "";
			// Replace global tags with real values
			stReturn = stReturn.replace("~~stPageTitle~", stPageTitle);

			String stA = this.ebEnt.ebUd.request.getParameter("a");
			if (stA == null || !stA.equals("28")) {
				double pageWidth = this.epsUd.rsMyDiv.getDouble("PageWidthPx");
				stReturn = stReturn.replaceAll("~PageWidthPx~",
						(pageWidth > 0 ? "" + pageWidth : "auto"));
			}
			if (iUserId > 0) {
				stReturn = stReturn
						.replace(
								"~~stWelcome~",
								"<div id='gen3'>"
										+ "<form method=post id=loginout name=loginout>"
										+ "<font class=medium>Welcome: </font><font class=mediumbold><a class='welcome-name' href='./?stAction=admin&t=9&do=edit&pk="
										+ this.iUserId
										+ "'>"
										+ this.ebEnt.dbDyn
												.ExecuteSql1("select concat(FirstName,' ',LastName) from Users where nmUserId="
														+ this.iUserId)
										+ "</a></font>"
										+ "<input type=hidden name=Logout value=Logout onClick=\"setSubmitId(9998);\">"
										+ "<div class='vsplitter'><span/></div>"
										+ "<span onClick='setSubmitId(9998);document.loginout.submit();' style='cursor:pointer;'><b>Sign Out</b></span>"
										+ "</form></div><div class='clr'><span/></div>");
				String stClass = "";
				Enumeration<String> paramNames = this.ebEnt.ebUd.request
						.getParameterNames();
				String queryString = this.ebEnt.ebUd.request.getQueryString();
				if (queryString != null && queryString.trim().length() > 0) {
					while (paramNames.hasMoreElements()) {
						String paramName = (String) paramNames.nextElement();
						if ("stsql".equals(paramName.toLowerCase()))
							continue;
						if (queryString.contains(paramName + "=")) {
							stClass += " "
									+ paramName
									+ "_"
									+ this.ebEnt.ebUd.request
											.getParameter(paramName);
						}
					}
				}
				if (stClass.trim().length() == 0)
					stClass = "stAction_home";
				stReturn = stReturn.replaceAll("~BodyStyleClass~", "body "
						+ stClass);
			} else {
				stReturn = stReturn.replace("~~stWelcome~", "");
				stReturn = stReturn
						.replaceAll("~BodyStyleClass~", "body-login");
			}

			stReturn = stReturn.replaceAll("~randomId~",
					"" + Math.abs(new Random().nextLong()));

			// add recent URL
			Set<String> keySet = recentURLs.keySet();
			String[] keyArr = new String[recentURLs.keySet().size()];
			recentURLs.keySet().toArray(keyArr);
			if (!stAction.equals("home")
					&& !this.ebEnt.ebUd.request.getQueryString().contains(
							"delete")) {
				int iContain = -1;
				if (stPageTitle != null && stPageTitle.trim().length() > 0) {
					for (int i = 0; i < keyArr.length; i++) {
						String key = keyArr[i];
						if (recentURLs.get(key).equals(
								this.ebEnt.ebUd.request.getQueryString())) {
							iContain = i;
							break;
						}
					}
					if (iContain >= 0) {
						for (int i = iContain + 1; i < keyArr.length; i++) {
							recentURLs.remove(keyArr[i]);
						}
					}
					if (recentURLs.containsKey(stPageTitle.toLowerCase())) {
						recentURLs.remove(stPageTitle.toLowerCase());
					}
					if (recentURLs.size() > 30) {
						keyArr = new String[recentURLs.keySet().size()];
						recentURLs.keySet().toArray(keyArr);
						recentURLs.remove(keyArr[0]);
					}
					recentURLs.put(stPageTitle.toLowerCase(),
							this.ebEnt.ebUd.request.getQueryString());
					this.ebEnt.ebUd.request.getSession().setAttribute(
							"recentURLs", recentURLs);
				}
			} else if (stAction.equals("home")) {
				recentURLs.clear();
			}

			String stRecents = "";
			keySet = recentURLs.keySet();
			if (keySet != null && keySet.size() > 0) {
				stRecents += "<ul id='nav-menu-home' class='ui-menu'>";
				for (String key : keySet) {
					stRecents += "<li><a href='./?" + recentURLs.get(key)
							+ "'>" + key + "</a></li>";
				}
				stRecents += "</ul>";
			}

			stReturn += "<script>"
					+ "jQuery(document).ready(function() {jQuery('#topNavItems .nav-home').append(\""
					+ stRecents
					+ "\");"
					+ "jQuery('#topNavItems').menubar({autoExpand: true,position: {my: 'left top',at: 'left bottom-2'}});"
					+ "fieldAutoComplete();" + "var sessionId = '"
					+ ((this.session != null) ? this.session.getId() : "")
					+ "';"
					// + ((EpsController.getRunningEOB() &&
					// !stAction.equals("runeob")) ? "catchAllTheLinks();" : "")
					+ "});" + "</script>";
		} catch (Exception e) {
			e.printStackTrace();
			this.stError += "<BR>ERROR getEpsPage: " + e;
		}
		return stReturn;
	}

	public String processRules(int nmFlag, String stContent) {
		String stReturn = "";
		if ((nmFlag & 0x04) != 0) {
			int iPos1 = 0;
			int iPos2 = 0;
			int iPos3 = 0; // last iPos2
			while ((iPos1 = stContent.indexOf("~~", iPos2)) >= 0) {
				iPos3 = iPos2;
				iPos2 = stContent.indexOf("~", iPos1 + 2);
				if (iPos2 > iPos1) {
					iPos2++; // advance past this character.
					stReturn += stContent.substring(iPos3, iPos1);
					String stCommand = stContent.substring(iPos1, iPos2);
					String[] asFields = stCommand.split("\\|");
					if (asFields.length >= 2) {
						if (asFields[0].equals("~~stPageTitle")) {
							stReturn += "~~stPageTitle~";
						} else if (asFields[0].equals("~~stWelcome")) {
							stReturn += "~~stWelcome~";
						} else if (asFields[0].equals("~~Version")) {
							stReturn += this.stVersion;
						} else if (asFields[0].equals("~~MenuBar")) {
							// Create Menu Bar.
							if (iUserId > 0) {
								stReturn += this.makeMenuBar();
							} else {
								stReturn += "&nbsp;";
							}
						} else if (asFields[0].equals("~~MainBody")) {
							if (iUserId <= 0) {
								this.epsUd.setPageTitle("Login");
								stReturn += this.epsUd.getLoginPage();
							} else {
								String stA = this.ebEnt.ebUd.request
										.getParameter("a");
								if (stA != null && stA.equals("28")) {
									this.epsUd
											.setPageTitle("Super User - Table Edit");
									stReturn += ebEnt.ebAdmin
											.getTableEdit(this.ebEnt.ebUd.request);
								} else {
									String stReturn1 = this.epsUd
											.getActionPage(stAction);
									String stChild = this.ebEnt.ebUd
											.getXlsProcess();
									if (stChild != null && stChild.length() > 0) {
										EpsXlsProject epsProject = new EpsXlsProject();
										epsProject.setEpsXlsProject(this.ebEnt,
												this.epsUd);
										if (stChild.equals("19"))
											stReturn1 = epsProject
													.xlsRequirementsEdit(stChild);
										else if (stChild.equals("21"))
											stReturn1 = epsProject
													.xlsSchedulesEdit(stChild);
										else if (stChild.equals("34"))
											stReturn1 = epsProject
													.xlsTestEdit(stChild);
										else if (stChild.equals("90"))
											stReturn1 = epsProject
													.xlsWBSEdit(stChild);
										else if (stChild.equals("46"))
											stReturn1 = epsProject.xlsAnalyze();
										else if (stChild.equals("26"))
											stReturn1 = epsProject
													.xlsBaseline(stChild);
										else if (stChild.equals("91"))
											stReturn1 = epsProject
													.translateProjectFile(
															stChild, "mpp");
										else if (stChild.equals("92"))
											stReturn1 = epsProject
													.translateProjectFile(
															stChild, "xer");
										else if (stChild.equals("93"))
											stReturn1 = epsProject
													.translateProjectFile(
															stChild, "ca");
										else if (stChild.equals("95"))
											stReturn1 = epsProject
													.xlsGanttChart(stChild);
										else
											stError += "<BR>ERROR child not implemented "
													+ stChild;
										this.stError += epsProject.getError();
									}
									stReturn += stReturn1;
								}
								if ("p".equals(this.ebEnt.ebUd.request
										.getParameter("d"))) {
									stReturn += "<script type='text/javascript'>";
									stReturn += "$(document).ready(function(){ printoutList(); });";
									stReturn += "</script>";
								}
							}
						}
					} else {
						stError += "<BR>ERROR: process command not implemented: "
								+ stCommand;
					}
				} else {
					break;
				}
			} // while
			if (iPos2 < stContent.length()) {
				stReturn += stContent.substring(iPos2); // copy remainder.
			}
		} else {
			stReturn = stContent;
		}
		return stReturn;
	}

	public String makeMenuBar() {
		String stReturn = "";
		stReturn += "<li class='top-nav nav-home ui-menubar-item'><a id='nav-home-item' class='topnav ui-menubar-link' href='./?stAction=home'>Home</a></li>";
		try {
			String stSql = "SELECT * FROM teb_table where nmTableType > 0 order by stTableName";
			ResultSet rs = this.ebEnt.dbDyn.ExecuteSql(stSql);
			rs.last();
			int iMax = rs.getRow();
			String stReport = "";
			String stAdmin = "";
			String stProject = "";
			for (int iR = 1; iR <= iMax; iR++) {
				rs.absolute(iR);
				if ((rs.getInt("nmProjectPriv") & nmPrivUser) != 0) {
					// stProject +=
					// "<li><a href='./?stAction=Projects&c=allocate'>Allocate</a></li>";
					stProject += "<li><a href='./?stAction=loadpage&m="
							+ URLEncoder.encode("Allocation Processing",
									"UTF-8")
							+ "&r="
							+ URLEncoder.encode(
									"./?stAction=Projects&c=allocate", "UTF-8")
							+ "'>Allocate</a></li>";
					stProject += "<li><a href='./?stAction=admin&t=12&do=insert'>Create Project</a></li>";
					// stProject +=
					// "<li><a href='./?stAction=Projects&c=analprj'>Analyze Project</a></li>";
					// stProject +=
					// "<li><a href='./?stAction=Projects&c=analreq'>Analyze Requirements</a></li>";
					// stProject +=
					// "<li><a href='./?stAction=Projects&c=analsch'>Analyze Schedule</a></li>";
					// stProject +=
					// "<li><a href='./?stAction=Projects&c=apprprj'>Approve Project</a></li>";
					// stProject +=
					// "<li><a href='./?stAction=Projects&c=apprreq'>Approve Requirement</a></li>";
					// stProject +=
					// "<li><a href='./?stAction=Projects&c=apprsch'>Approve Schedule</a></li>";
					// stProject +=
					// "<li><a href='./?stAction=Projects&c=critscor'>Criterion Scoring</a></li>";
					if (epsUd.stPrj != null && epsUd.stPrj.length() > 0
							&& !epsUd.stPrj.equals("0")) {
						ResultSet rsProject = this.ebEnt.dbDyn
								.ExecuteSql("SELECT * FROM Projects WHERE RecId="
										+ epsUd.stPrj);
						if (rsProject.next()) {
							boolean isTemplate = rsProject
									.getBoolean("isTemplate");
							String stPrjLink = "./?stAction=Projects&t=12&pk="
									+ this.epsUd.stPrj;
							stProject += "<li><a href='javascript:void(0);'>Current Project</a></li>"

									+ "	<li><a class='sub-item' href='./?stAction=loadpage&m="
									+ URLEncoder
											.encode("Analyzing Project - "
													+ rsProject
															.getString("ProjectName"),
													"UTF-8")
									+ "&r="
									+ URLEncoder.encode(stPrjLink
											+ "&do=xls&child=46", "UTF-8")
									+ "'>Analyze Project</a></li>"

									+ "	<li><a class='sub-item' href='"
									+ stPrjLink
									+ "&do=approve'>Approve</a></li>"
									+ (!isTemplate ? "	<li><a class='sub-item' href='"
											+ stPrjLink
											+ "&do=xls&child=26'>Baseline</a></li>"
											+ "	<li><a class='sub-item' href='./?stAction=Projects&c=critscor'>Criterion Score</a></li>"
											: "")
									+ "	<li><a class='sub-item' href='"
									+ stPrjLink
									+ "&do=dup'>Duplicate</a></li>"
									+ (!isTemplate ? "	<li><a class='sub-item' href='"
											+ stPrjLink
											+ "&do=xls&child=95'>Gantt</a></li>"
											: "")
									+ "	<li><a class='sub-item' href='"
									+ stPrjLink
									+ "&do=edit'>Project Attributes</a></li>"
									+ "	<li><a class='sub-item' href='"
									+ stPrjLink
									+ "&do=xls&child=19'>Requirements</a></li>"
									+ "	<li><a class='sub-item' href='"
									+ stPrjLink
									+ "&do=xls&child=21'>Schedule</a></li>"
									+ "<li><a class='sub-item collapsed' href='javascript:void(0);'>Translate Schedule</a><ul class='ui-menu'>"
									+ "  <li><a href='"
									+ stPrjLink
									+ "&do=xls&child=93'>Clarity</a></li>"
									+ "  <li><a href='"
									+ stPrjLink
									+ "&do=xls&child=91'>MS Project</a></li>"
									+ "  <li><a href='"
									+ stPrjLink
									+ "&do=xls&child=92'>Primavera</a></li>"
									+ "</ul></li>"
									+ "	<li><a class='sub-item' href='"
									+ stPrjLink
									+ "&do=xls&child=90'>WBS</a></li>";
						}
					}
					stProject += "<li><a href='./?stAction=Projects&t=96&do=insert'>Instantiate</a></li>";
					stProject += "<li><a href='./?stAction=Projects&t="
							+ rs.getInt("nmTableId") + "'>"
							+ rs.getString("stTableName") + "</a></li>";
					stProject += "<li><a href='./?stAction=Projects&t=94&do=insert'>Template</a></li>";
				}
				if ((rs.getInt("nmReportPriv") & nmPrivUser) != 0) {
					String stLink = "./?stAction=reports&t="
							+ rs.getInt("nmTableId");
					String stViewLink = stLink + "&submit2="
							+ URLEncoder.encode("View Saved Reports", "UTF-8");
					String stRunLink = stLink + "&submit2="
							+ URLEncoder.encode("Run/Execute Report", "UTF-8");
					String stCustomLink = stLink
							+ "&submit2="
							+ URLEncoder.encode("Custom Report Designer",
									"UTF-8");
					ResultSet rsR = epsUd.ebEnt.dbDyn
							.ExecuteSql("SELECT * FROM teb_customreport where stReportType = '"
									+ rs.getString("nmTableId")
									+ "' order by nmDefaultReport DESC,stReportName limit 0,30");

					String stRun = "";
					String stCustom = "";
					while (rsR.next()) {
						stRun += "<li><a href='" + stRunLink + "&customreport="
								+ rsR.getString("RecId") + "'>"
								+ rsR.getString("stReportName") + "</a></li>";
						if (stCustom.length() == 0
								&& rsR.getInt("nmDefaultReport") > 0)
							stCustom += stCustomLink + "&customreport="
									+ rsR.getString("RecId");
					}
					stReport += "<li><a class='collapsed' href='"
							+ stLink
							+ "'>"
							+ rs.getString("stTableName")
							+ "</a>"
							+ "<ul id='nav-menu-reports-"
							+ rs.getString("stTableName").toLowerCase()
									.replaceAll(" ", "-")
							+ "' class='ui-menu'>"
							+ "<li><a href='"
							+ stCustom
							+ "&canedit=1'>Custom Report Designer</a></li>"
							+ "<li><a class='collapsed' href='javascript:void(0);'>Run/Execute Report</a><ul id='nav-menu-reports-"
							+ rs.getString("stTableName").toLowerCase()
									.replaceAll(" ", "-")
							+ "-run/execute-report' class='ui-menu'>" + stRun
							+ "</ul></li>" + "<li><a href='" + stViewLink
							+ "'>View Saved Reports</a></li>" + "</ul>"
							+ "</li>";
				}
				if ((rs.getInt("nmAccessPriv") & nmPrivUser) != 0) {
					if (stAdmin.length() <= 0 && (nmPrivUser & 0x220) != 0) // Ex
																			// and
																			// PPM
						stAdmin += "<li><a href='./?stAction=admin&c=appr'>Approve Special Days</a></li>";

					if (rs.getInt("nmTableId") == 15) // Options
						stAdmin += "<li><a href='./?stAction=admin&t="
								+ rs.getInt("nmTableId") + "&pk=1&do=edit'>"
								+ rs.getString("stTableName") + "</a></li>";
					else if (rs.getInt("nmTableId") == 9) // Users
					{
						stAdmin += "<li><a href='./?stAction=admin&t="
								+ rs.getInt("nmTableId") + "&do=users'>"
								+ rs.getString("stTableName") + "</a></li>";
					} else {
						stAdmin += "<li><a href='./?stAction=admin&t="
								+ rs.getInt("nmTableId") + "'>"
								+ rs.getString("stTableName") + "</a></li>";
					}
				}
			}
			if (stReport.length() > 0)
				stReturn += "<li class='top-nav nav-reports ui-menubar-item'><a id='nav-reports-item' class='topnav ui-menubar-link' href='javascript:void(0);'>Reports</a><ul id='nav-menu-reports' class='ui-menu'>"
						+ stReport + "</ul></li>";

			if (stProject.length() > 0)
				stReturn += "<li class='top-nav nav-project ui-menubar-item'><a id='nav-project-item' class='topnav ui-menubar-link' href='javascript:void(0);'>Project</a><ul id='nav-menu-project' class='ui-menu'>"
						+ stProject + "</ul></li>";

			if (stAdmin.length() > 0)
				stReturn += "<li class='top-nav nav-administration ui-menubar-item'><a id='nav-administration-item' class='topnav ui-menubar-link' href='javascript:void(0);'>Administration</a><ul id='nav-menu-administration' class='ui-menu'>"
						+ stAdmin + "</ul></li>";
			stReturn += "<li class='top-nav nav-edit ui-menubar-item'><a id='nav-edit-item' class='topnav ui-menubar-link' href='javascript:void(0);'>Edit</a><ul id='nav-menu-edit' class='ui-menu'>edit-menu-content</ul></li>";

			stReturn += "<li class='top-nav nav-help ui-menubar-item'><a id='nav-help-item' class='topnav ui-menubar-link' href='javascript:void(0);'>Help</a><ul id='nav-menu-help' class='ui-menu'>";
			stReturn += "<li><a href='./?stAction=help&i=about'>About</a></li>";
			stReturn += "<li><a href='./?stAction=help&i=content'>Contents</a></li></ul></li>";

			if ((this.ebEnt.ebUd.getLoginPersonFlags() & 0x800) != 0) { // Super
																		// User
																		// only
				stReturn += "<li class='top-nav nav-system-admin ui-menubar-item'><a id='nav-system-admin-item' class='topnav ui-menubar-link' href='#'>System Admin</a><ul id='nav-menu-system-admin' class='ui-menu'>";
				// stReturn +=
				// "<li><a href='./?a=28&tb=1.d.teb_division'>Division Setup</a></li>";
				stReturn += "<li><a href='./?stAction=tcimport&id2=di&submit=LOAD&stopEOB=true&ta="
						+ URLEncoder
								.encode("::divivsion\n\n::LABORCATEGORIES\n\n::user\n\n::Projects\n\n::Schedule\n\n::REQUIREMENTS\n\n::TestPlan",
										"UTF-8") + "'>Clear Database</a></li>";
				stReturn += "<li><a href='./?stAction=tcimport&id2=di'>Data Import</a></li>";
				// stReturn +=
				// "<li><a href='./?stAction=specialdays'>Load Site Special Days</a></li>";
				// stReturn +=
				// "<li><a target=\"_blank\" href='./?b=2002'>QA Admin</a></li>";
				stReturn += "<li><a href='./?a=28&tb=1.e.X25RefContent'>Edit Content</a></li>";
				stReturn += "<li><a href='./?stAction=d50d53&commtrace=i'>Full D50/D53</a></li>";
				stReturn += "<li><a href='./?stAction=loaddb&id2=di'>Load DB</a></li>";
				stReturn += "<li><a target=\"_blank\" href='./?b=2001'>QA Manager</a></li>";
				/* 
				 * Suin Start
				 * Adding menu items in systemAdmin tab  
				 */
				stReturn += "<li><a href='./?stAction=loaddictionary'>Load Dictionary</a></li>";
				stReturn += "<li><a href='./?stAction=savedictionary'>Save Dictionary</a></li>";
				/*
				 * Suin End
				 */
				stReturn += "<li><a href='./?stAction=rescyneb'>Resync EB</a></li>";
				stReturn += "<li><a href='./?stAction=loadpage&m="
						+ URLEncoder.encode("End-of-Business Processing",
								"UTF-8")
						+ "&r="
						+ URLEncoder.encode(
								"./?stAction=runeob" /* "&commtrace=i" */,
								"UTF-8") + "'>Run EOB now</a></li>";
				stReturn += "<li><a href='./?stAction=loadpage"
						+ "&r="
						+ URLEncoder.encode("./?stAction=savedb&id2=di",
								"UTF-8")
						+ "&m="
						+ URLEncoder.encode("Back-up Databases in progress",
								"UTF-8") + "'>Save DB</a></li>";
				stReturn += "<li><a href='./?stAction=tablefield&rebuild=tblcol'>Table/Field Manager</a></li>";
				stReturn += "</ul></li>";
			}
		} catch (Exception e) {
			this.stError += "<BR> makeMenuBar " + e;
		}
		return stReturn;
	}

	public String getError() {
		if (this.epsUd != null)
			this.stError += this.epsUd.getError();

		return this.stError;
	}
}
