import geb.Browser
import groovy.json.JsonOutput
import org.openqa.selenium.htmlunit.HtmlUnitDriver

email = ""
password = ""
projectName = ""
targetFile = new File("$projectName-issues.json")

firstExported = 0
numExported = 99999
columnNames = ["ID", "Type", "Module", "Status", "Milestone", "Reporter", "Owner", "Summary",
  "Priority", "Attachments", "Stars", "Opened", "Closed", "Modified", "BlockedOn", "Blocking", "Blocked",
  "MergedInto", "Cc", "Project"]
browser = new Browser(driver: new HtmlUnitDriver())
issues = []

login()
extractOverviews()
extractDetails()
writeJson()

def login() {
  if (!email) {
    println "Skipping login as no email was given"
    return
  }

  browser.go("https://accounts.google.com/ServiceLogin?sacu=1")
  browser.Email = email
  browser.Passwd = password
  browser.signIn().click()
  if (browser.$("#errormsg_0_Passwd")) {
    throw new Exception("Wrong email or password")
  }
  println "Login successful"
}

def extractOverviews() {
  browser.go("https://code.google.com/p/$projectName/issues/list?can=1&q=&sort=\
&groupby=&colspec=${columnNames.join("+")}&num=$numExported&start=$firstExported")
  def rows = browser.$("#resultstable tbody tr")
  issues = rows.collect { row ->
    try {
      def cells = row.find("td")
      def cellValues = cells.collect { it.find("a").text() }.findAll { it != null }
      assert cellValues.size() == columnNames.size()
      def issue = [columnNames, cellValues].transpose().collectEntries { it }
      println "Extracted overview for issue ${issue["ID"]}: ${issue["Summary"]}"
      issue
    } catch (Exception e) {
      println "Failed to extract issue overview: $e"
      null
    }
  }.findAll { it != null }
}

def extractDetails() {
  issues.each { issue ->
    try {
      browser.go("https://code.google.com/p/$projectName/issues/detail?id=${issue["ID"]}")
      issue["Labels"] = browser.$("a.label")*.text()
      def issueDescription = browser.$("td.issuedescription")
      def comments = issueDescription.find("div.issuedescription") + issueDescription.find("div.issuecomment")
      issue["Comments"] = comments.collect { comment ->
        [
            Author: comment.find(".author a.userlink").text(),
            Date: comment.find("span.date").attr("title"),
            Text: comment.find("pre").text(),
            Attachments: comment.find("div.attachments").collect { attachment ->
              [
                  Name: attachment.find("td b").text(),
                  DownloadUrl: attachment.find("a", text: "Download").attr("href")
              ]
            }
        ]
      }
      println "Extracted details for issue ${issue["ID"]}: ${issue["Summary"]}"
    } catch (Exception e) {
      println "Failed to extract details for issue ${issue["ID"]}: $e"
    }
  }
}

def writeJson() {
  targetFile.setText(JsonOutput.prettyPrint(JsonOutput.toJson(issues)), "utf-8")
  println "Exported ${issues.size()} issues to $targetFile.absolutePath"
}

