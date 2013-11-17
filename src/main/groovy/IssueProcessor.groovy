import groovy.json.JsonOutput
import groovy.json.JsonSlurper

projectName = ""
sourceFile = new File("$projectName-issues.json")
targetFile = new File("$projectName-issues-processed.json")

issues = new JsonSlurper().parse(sourceFile, "utf-8")
processEmail()
targetFile.setText(JsonOutput.prettyPrint(JsonOutput.toJson(issues)), "utf-8")

def processEmail() {
  issues.each { issue ->
    if (!issue["Reporter"].contains("@")) {
      issue["Reporter"] += "@gmail.com"
    }
    if (!issue["Owner"].contains("@")) {
      issue["Owner"] += "@gmail.com"
    }
    issue["Comments"].each { comment ->
      if (!comment["Author"].contains("@")) {
        comment["Author"] += "@gmail.com"
      }
    }
  }
}





