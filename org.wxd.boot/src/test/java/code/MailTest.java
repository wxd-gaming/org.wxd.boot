package code;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-02-09 17:23
 **/
public class MailTest {

    /** gmail邮箱SSL方式 */
    private void gmailSsl(Properties props) {
        final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
//        props.put("mail.debug", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.socketFactory.class", SSL_FACTORY);
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.auth", "true");
    }

    /** gmail邮箱的TLS方式 */
    private void gmailTls(Properties props) {
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
    }

    /** 通过gmail邮箱发送邮件 */
    public void gmailSender(String email, String title, String text) {
        /*Get a Properties object */
        Properties props = new Properties();
        /*选择ssl方式*/
        gmailTls(props);

        final String username = "jackwei415888@gmail.com";        /*gmail邮箱*/
        final String password = "LIUliBO890427123";        /*密码*/

        Session session = Session.getDefaultInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                }
        );
        // -- Create a new message --
        Message msg = new MimeMessage(session); // -- Set the FROM and TO fields --
        try {
            msg.setFrom(new InternetAddress(username));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            msg.setSubject("");
            msg.setText("");
            msg.setSentDate(new Date());
            Transport.send(msg);
        } catch (AddressException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        System.out.println("Message sent.");
    }

    public static void main(String[] args) {
        new MailTest().gmailSender("492794628@qq.com", "rrr", "dddddd");
    }

}
