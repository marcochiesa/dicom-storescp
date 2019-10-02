package org.getmarco.storescp;

import lombok.Value;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;

/**
 * Simple value class to hold metadata about a transfered study. Serialized to
 * a metadata file using the Jackson {@link com.fasterxml.jackson.databind.ObjectMapper ObjectMapper}.
 */
@Value
public class MetaData {
    String patientId;
    String patientName;
    String patientDob;
    String accessionNumber;
    String studyUid;
    String studyDesc;
    String modality;

    public MetaData(Attributes attributes) {
        this.patientId = attributes.getString(Tag.PatientID);
        this.patientName = attributes.getString(Tag.PatientName);
        this.patientDob = attributes.getString(Tag.PatientBirthDate);
        this.accessionNumber = attributes.getString(Tag.AccessionNumber);
        this.studyUid = attributes.getString(Tag.StudyInstanceUID);
        this.studyDesc = attributes.getString(Tag.StudyDescription);
        this.modality = attributes.getString(Tag.Modality);
    }
}
