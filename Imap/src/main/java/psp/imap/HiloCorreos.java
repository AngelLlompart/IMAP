/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package psp.imap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JProgressBar;

/**
 *
 * @author lawde
 */
public class HiloCorreos implements Runnable{
    private JLabel totalCorreos;
    private String correosPath;
    private JProgressBar progressBar;
    private JButton btnSelectFile;
    private JButton btnReadCorreos;
    private static File directorio;
    private static DefaultListModel correosListModel;
    private static JList lstCorreos;
    public HiloCorreos(JLabel totalCorreos, String correosPath, JProgressBar progressBar, JButton btnSelectFile, JButton btnReadCorreos, File directorio, DefaultListModel correosListModel, JList lstCorreos) {
        this.totalCorreos = totalCorreos;
        this.correosPath = correosPath;
        this.progressBar = progressBar;
        this.btnSelectFile = btnSelectFile;
        this.btnReadCorreos = btnReadCorreos;
        this.directorio = directorio;
        this.correosListModel = correosListModel;
        this.lstCorreos = lstCorreos;
    }
    
    @Override
    public void run() {
        try {
            leerCorreos(totalCorreos, correosPath, progressBar, btnSelectFile, btnReadCorreos);
        } catch (MessagingException ex) {
            Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void leerCorreos(JLabel totalCorreos, String correosPath, JProgressBar prgCorreos, JButton btnSelectFile, JButton btnReadCorreos) throws MessagingException, IOException {
        cargarLista();
        Folder folder = null;
        Store store = null;
        try {
            Properties properties = new Properties();

            // server setting
            properties.put("mail.imap.host", "imap.gmail.com");
            properties.put("mail.imap.port", "993");

            // SSL setting
            properties.setProperty("mail.imap.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail.imap.socketFactory.fallback",
                    "false");
            properties.setProperty("mail.imap.socketFactory.port",
                    String.valueOf(993));

            properties.setProperty("mail.store.protocol", "imaps");

            Session session = Session.getDefaultInstance(properties);
            store = session.getStore("imaps");
            store.connect("imap.gmail.com", "pruebaspsp111@gmail.com", "pspimap123");
            folder = store.getFolder("Inbox");

            folder.open(Folder.READ_WRITE);
            Message messages[] = folder.getMessages();
            int total = folder.getMessageCount();
            totalCorreos.setText("Número total de mensages: " + total);
            //System.out.println("No of Unread Messages : " + folder.getUnreadMessageCount());
            for (int i = 0; i < messages.length; ++i) {

                Message msg = messages[i];
                
                //if we don''t want to fetch messages already processed
        
                //if (!msg.isSet(Flags.Flag.SEEN)) {
                    /*Enumeration headers = messages[i].getAllHeaders();
                    while (headers.hasMoreElements()) {
                        Header h = (Header) headers.nextElement();
                        System.out.println(h.getName() + ": " + h.getValue());
                    }
                    System.out.println();*/
  
                    System.out.println("MESSAGE #" + (i + 1) + ":");
                    String from = "unknown";
                    System.out.println(msg.getHeader("Content-type"));
                    System.out.println(msg.getHeader("format"));
                    System.out.println(msg.getHeader("Content-Transfer-Encoding"));
                    if (msg.getReplyTo().length >= 1) {
                        from = msg.getReplyTo()[0].toString();
                    } else if (msg.getFrom().length >= 1) {
                        from = msg.getFrom()[0].toString();
                    }
                    String subject = msg.getSubject();
                    //System.out.println("Saving ... " + subject +" " + from);
                    // you may want to replace the spaces with "_"
                    // the TEMP directory is used to store the files
                    String filename = correosPath + "/" + subject;
                    saveParts(msg.getContent(), filename, subject);

                    msg.setFlag(Flags.Flag.SEEN, true);

                    
                    // to delete the message
                    // msg.setFlag(Flags.Flag.DELETED, true);
                /*} else {

                    System.out.println("MESSAGE #" + (i + 1) + ":" + " is already saved.");

                    String subject = msg.getSubject();
                }*/
                prgCorreos.setValue((i + 1) * prgCorreos.getMaximum() / (total));
            }
            btnSelectFile.setEnabled(true);
            btnReadCorreos.setEnabled(true);

        } finally {
            if (folder != null) {
                folder.close(true);
            }
            if (store != null) {
                store.close();
            }
        }
    }

    private void saveParts(Object content, String filename, String subject) throws IOException, MessagingException {
        OutputStream out = null;
        InputStream in = null;
        try {
            if (content instanceof Multipart) {
                Multipart multi = ((Multipart) content);
                int parts = multi.getCount();
                for (int j = 0; j < parts; ++j) {
                    MimeBodyPart part = (MimeBodyPart) multi.getBodyPart(j);
                    if (part.getContent() instanceof Multipart) {
                        // part-within-a-part, do some recursion...
                        saveParts(part.getContent(), filename, subject);
                    } else {
                        String extension = "";
                        if (part.isMimeType("text/html")) {
                            extension = "html";
                        } else {
                            if (part.isMimeType("text/plain")) {
                                extension = "txt";
                            } else {
                                //  Try to get the name of the attachment
                                extension = part.getDataHandler().getName();
                            }
                            filename = filename + "." + extension;
                            System.out.println("... " + filename);
                            //cargarLista();
                            //con esto el .txt.null no se pone y no se puede leer, mejor sera cargarlista de 0 aunque tarde mas? idk
                            correosListModel.addElement(subject + "." + extension);
                            lstCorreos.setModel(correosListModel);
                            out = new FileOutputStream(new File(filename), true);
                            in = part.getInputStream();
                            int k;
                            while ((k = in.read()) != -1) {
                                out.write(k);
                            }
                        }
                    }
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
    
    private void cargarLista() {
        //correosListModel.clear();
        for (File f : directorio.listFiles()) {
            if(!f.isDirectory()){
                correosListModel.addElement(f.getName());
            }
        }
        lstCorreos.setModel(correosListModel);
    }
    
}
