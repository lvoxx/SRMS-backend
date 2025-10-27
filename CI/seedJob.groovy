// Load jobs
new File("${WORKSPACE}/CI/jobs").eachFileMatch(~/.*\.groovy/) { file ->
    println "Loading job: ${file.name}"
    evaluate(file.text)
}

// Load configs
new File("${WORKSPACE}/CI/config").eachFileMatch(~/.*\.groovy/) { file ->
    println "Loading config: ${file.name}"
    evaluate(file.text)
}