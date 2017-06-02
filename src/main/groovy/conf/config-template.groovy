serverURL = 'http://localhost:4567'
codes {
    recursive {
        github {
            user = ''
        }
        email = ''
        dateFormat = 'M/d/yyyy h:mm a z'
        jsDateTimeFormat = 'MM/DD/YYYY hh:mm A'
        sourcePath = '/path/to/src/main/groovy/' // used for displaying a relevant code snippet on error pages in dev environment
    }
}

datasource {
    url = ''
    username = ''
    password = ''
    driverClassName = ''
}

awssdk {
    s3 {
        accessKey = ''
        secretKey = ''
    }
    ses {
        accessKey = ''
        secretKey = ''
    }
}