package server;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

@XmlRootElement
public class Settings {
    private final static String fileXml = "/Users/inga/Documents/IdeaProjects/chat/src/server/settings.xml";
    private int port;

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
            marshaller.marshal(ServerController.settings, new File(fileXml));
            marshaller.marshal(ServerController.settings, System.out);
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
            setPort(settings.port);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
