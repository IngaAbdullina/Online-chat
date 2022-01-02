package client;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

@XmlRootElement
public class Settings {
    private final static String fileXml = "/Users/inga/Documents/IdeaProjects/chat/src/client/settings.xml";
    private String user;
    private String server;
    private int port;

    public String getUser() {
        return this.user;
    }

    @XmlElement
    public void setUser(String user) {
        this.user = user;
    }

    public String getServer() {
        return this.server;
    }

    @XmlElement
    public void setServer(String server) {
        this.server = server;
    }

    public int getPort() {
        return this.port;
    }

    @XmlElement
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Записываем данные в XML
     */
    public void saveToXml() {
        try {
            JAXBContext context = JAXBContext.newInstance(Settings.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(ClientController.settings, new File(fileXml));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    /**
     * Загружаем данные из XML и сохраняем в переменные класса Settings
     */
    public void loadFromXml() {
        try {
            JAXBContext context = JAXBContext.newInstance(Settings.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Settings settings = (Settings) unmarshaller.unmarshal(new File(fileXml));
            setUser(settings.user);
            setServer(settings.server);
            setPort(settings.port);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
