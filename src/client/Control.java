package client;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

@XmlRootElement
public class Control {
    private final static String fileXml = "/Users/inga/Documents/IdeaProjects/chat/src/client/control.xml";
    private String controlWord;

    public String getControlWord() { return this.controlWord; }

    @XmlElement
    public void setControlWord(String controlWord) { this.controlWord = controlWord; }

    /**
     * Записываем данные в XML
     */
    public void saveToXml() {
        try {
            JAXBContext context = JAXBContext.newInstance(Control.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(ClientController.control, new File(fileXml));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    /**
     * Загружаем данные из XML и сохраняем в переменные класса Settings
     */
    public void loadFromXml() {
        try {
            JAXBContext context = JAXBContext.newInstance(Control.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Control control = (Control) unmarshaller.unmarshal(new File(fileXml));
            setControlWord(control.controlWord);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
