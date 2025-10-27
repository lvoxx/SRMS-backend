new File('CI/jobs').eachFileMatch(~/.*\.groovy/) { file ->
    evaluate(file)
}