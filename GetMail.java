import java.io.IOException;
import java.util.Properties;

import javax.mail.*;
import javax.mail.search.FlagTerm;

/**
 * Retrieves unopened mail for a given user and server, either the contents of a specific message or a summary of all unopened messages
 *
 * @author Tyler Matthews
 */
public class GetMail {
   /**
    * The server to connect to
    */
   private String server;

   /**
    * The user whose mail to retrieve
    */
   private String user;
   /**
    * The password of the user
    */
   private String pw;
   /**
    * Mail number to retrieve, or flag to indicate none
    */
   private int    mailNumToRetrieve;

   /**
    * Intiailizes GetMail
    *
    * @param server The server to retrieve mail from
    * @param user The user of the account to retrieve mail from
    * @param pw The password of the user
    * @param mailNumToRetrieve Optional mail number to print out
    */
   public GetMail(String server, String user, String pw, int mailNumToRetrieve) {
      this.server = server;
      this.user = user;
      this.pw = pw;
      this.mailNumToRetrieve = mailNumToRetrieve;
   }

   /**
    * Gets mail, making a connection to mail server and printing out appropriate message content
    *
    * @throws MessagingException on mesage exception
    * @throws IOException on io exception
    */
   private void run() throws MessagingException, IOException {
      Properties props = new Properties();
      props.put("mail.store.protocol", "imaps");
      Session session = Session.getDefaultInstance(props);
      Store store = session.getStore();
      store.connect(server, user, pw);
      Folder emailFolder = store.getFolder("inbox"); // we want the inbox
      emailFolder.open(Folder.READ_ONLY); // read only, don't mark read after opening
      if (mailNumToRetrieve == -1) {
         // if flag for all, get all
         getAllMail(emailFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false)));
      } else {
         // else get the specific email and print out body conets
         getMail(emailFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false)));
      }
      emailFolder.close(false);
      store.close();
   }

   /**
    * This method was taken in its entirety from: https://javaee.github.io/javamail/FAQ#mainbody Return the primary text content of the
    * message, it handles the retrieving body text of a multipart message and accompanying edge cases
    */
   private String getText(Part p) throws MessagingException, IOException {
      if (p.isMimeType("text/*")) {
         String s = (String) p.getContent();
         return s;
      }
      if (p.isMimeType("multipart/alternative")) {
         // prefer html text over plain text
         Multipart mp = (Multipart) p.getContent();
         String text = null;
         for (int i = 0; i < mp.getCount(); i++) {
            Part bp = mp.getBodyPart(i);
            if (bp.isMimeType("text/plain")) {
               if (text == null)
                  text = getText(bp);
               continue;
            } else if (bp.isMimeType("text/html")) {
               String s = getText(bp);
               if (s != null)
                  return s;
            } else {
               return getText(bp);
            }
         }
         return text;
      } else if (p.isMimeType("multipart/*")) {
         Multipart mp = (Multipart) p.getContent();
         for (int i = 0; i < mp.getCount(); i++) {
            String s = getText(mp.getBodyPart(i));
            if (s != null)
               return s;
         }
      }
      return null;
   }

   /**
    * Ptints out the subject/from info for each message
    *
    * @param messages The messages to display
    * @throws MessagingException on message exception
    * @throws IOException on io exception
    */
   private void getAllMail(Message[] messages) throws MessagingException, IOException {
     if (messages.length > 0) {
       for (int x = 0; x < messages.length; x++) {
          Message message = messages[x];
          System.out.println((x + 1) + ". " + message.getSubject() + " (" + message.getFrom()[0] + ")");
       }
     } else {
       System.out.println("No unread messages :)");
     }
   }

   /**
    * Prints out the content of the index message number
    *
    * @param messages The list of unopened messages
    * @throws MessagingException on message exception
    * @throws IOException on io exception
    */
   private void getMail(Message[] messages) throws MessagingException, IOException {
      try {
         Message message = messages[mailNumToRetrieve - 1]; // get message
         System.out.println(getText(message));
      } catch (ArrayIndexOutOfBoundsException ex) {
         System.out.println("Message does not exist"); // if out bounds, it didn't exist
      }
   }

   /**
    * Get Mail expects the server to get mail from, the user/pw combiniation and an optional email unopened email to print out
    *
    * @param args The arguments, to the server to get mail from, the user/pw combiniation and an optional email unopened email to print out
    */
   public static void main(String[] args) {
      try {
         if (args.length > 4 || args.length < 3) {
            throw new ArrayIndexOutOfBoundsException();
         }
         GetMail get = new GetMail(args[0], args[1], args[2], args.length == 4 ? Integer.parseInt(args[3]) : -1);
         get.run();
      } catch (ArrayIndexOutOfBoundsException ex) {
         System.out.println("Usage: java GetMail server user password <email_to_retrieve>");
      } catch (Exception ex) {
         System.out.println(ex.getMessage());
      }
   }
}
