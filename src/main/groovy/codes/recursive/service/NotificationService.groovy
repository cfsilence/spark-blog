package codes.recursive.service

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model.Body
import com.amazonaws.services.simpleemail.model.Content
import com.amazonaws.services.simpleemail.model.Destination
import com.amazonaws.services.simpleemail.model.Message
import com.amazonaws.services.simpleemail.model.SendEmailRequest

/**
 * Created by toddsharp on 4/18/17.
 */
class NotificationService {
    BasicAWSCredentials awsCreds

    NotificationService(accessKey, secretKey){
        awsCreds = new BasicAWSCredentials(accessKey, secretKey)
    }

    def send(to, from, subject, body) {
        Destination destination = new Destination().withToAddresses([to])
        Content s = new Content().withData(subject)
        Content textBody = new Content().withData(body);
        Body b = new Body().withText(textBody)
        Message message = new Message().withSubject(s).withBody(b)
        SendEmailRequest request = new SendEmailRequest().withSource(from).withDestination(destination).withMessage(message)
        AmazonSimpleEmailServiceClient client = AmazonSimpleEmailServiceClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds)).withRegion(Regions.US_EAST_1).build()
        return client.sendEmail(request)
    }
}
