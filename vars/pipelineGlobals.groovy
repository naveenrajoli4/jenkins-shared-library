def getAccountID(String environment){
    switch(environment) { 
        case 'dev': 
            return "135808959960"
        case 'qa':
            return "315069654700"
        case 'uat':
            return "315069654700"
        case 'pre-prod':
            return "315069654700"
        case 'prod':
            return "135808959960"
        default:
            return "nothing"
    } 
}