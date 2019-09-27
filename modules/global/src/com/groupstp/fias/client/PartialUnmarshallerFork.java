package com.groupstp.fias.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static javax.xml.stream.XMLStreamConstants.*;

public class PartialUnmarshallerFork<T> implements Iterator<T>, Closeable {
    private final XMLStreamReader reader;
    private final Class<T> destinationClass;
    private final Unmarshaller unmarshaller;
    private final ProgressCounterFilterInputStream inputStream;
    private long offset;
    //для хранения прогресса повторного чтения для вывода в лог
    private int lastPercentValue;
    private int percentValue;

    private static final Logger log = LoggerFactory.getLogger("FiasClient");

    @Deprecated
    public PartialUnmarshallerFork(ProgressCounterFilterInputStream inputStream, Class<T> destinationClass) throws Exception {
        this.inputStream = inputStream;
        this.destinationClass = destinationClass;
        this.unmarshaller = JAXBContext.newInstance(destinationClass).createUnmarshaller();
        this.reader = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);

        // ignore headers
        skipElements(START_DOCUMENT, DTD);
        // ignore root element
        reader.nextTag();
        // if there's no tag, ignore root element's end
        skipElements(END_ELEMENT);
    }

    public PartialUnmarshallerFork(ProgressCounterFilterInputStream inputStream, Class<T> destinationClass, long offset) throws Exception {
        this.inputStream = inputStream;
        this.destinationClass = destinationClass;
        this.unmarshaller = JAXBContext.newInstance(destinationClass).createUnmarshaller();
        this.reader = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
        this.offset = offset;
        this.percentValue = 0;

        // ignore headers
        skipElements(START_DOCUMENT, DTD);
        // ignore root element
        reader.nextTag();
        // if there's no tag, ignore root element's end
        skipElements(END_ELEMENT);
        //skip beginning of the document (if it was parsed earlier)
        skipBeginning(this.offset);
    }

    public ProgressCounterFilterInputStream getInputStream() {
        return inputStream;
    }

    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        try {
            T value = unmarshaller.unmarshal(reader, destinationClass).getValue();
            skipElements(CHARACTERS, END_ELEMENT);
            return value;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasNext() {
        try {
            return reader.hasNext();
        } catch (XMLStreamException e) {
            return false;
        }
    }

    public void close() throws IOException {
        try {
            reader.close();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private void skipElements(Integer... elements) throws Exception {
        int eventType = reader.getEventType();

        List<Integer> types = Arrays.asList(elements);
        while (types.contains(eventType))
            eventType = reader.next();
    }

    private void skipBeginning(long offset) throws Exception {
        while (this.inputStream.getProgress() <= offset) {
            reader.next();
            lastPercentValue = this.percentValue;
            this.percentValue = (int) ((float) this.inputStream.getProgress() / (float) offset * 100);
            if (this.percentValue > lastPercentValue)
                log.info("Searching last position of parsing, reached {}% of last position",
                        this.percentValue);
        }
    }
}
