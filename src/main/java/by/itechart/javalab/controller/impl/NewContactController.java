package by.itechart.javalab.controller.impl;

import by.itechart.javalab.controller.Controller;
import by.itechart.javalab.controller.ControllerException;
import by.itechart.javalab.entity.*;
import by.itechart.javalab.service.ContactAttributesService;
import by.itechart.javalab.service.ModificationContactService;
import by.itechart.javalab.service.ServiceException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class NewContactController implements Controller {
    private static Logger log = LogManager.getLogger(NewContactController.class.getName());
    private Map<String, String> formFields = new HashMap<>();
    private static final int MAX_FILE_SIZE = 1024 * 1024 * 100;
    private static final int MAX_REQUEST_SIZE = 1024 * 1024 * 300;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            List<String> countries = ContactAttributesService.getAllCountries();
            request.setAttribute("countries", countries);
            request.getServletContext().getRequestDispatcher("/WEB-INF/pages/newContact.jsp").forward(request, response);
        } catch (ServiceException | ServletException | IOException e) {
            log.error(e);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {
            log.debug("Not multipart type request: " + request.getRequestURI() + " " + request.getContentType());
            return;
        }
        DiskFileItemFactory factory = new DiskFileItemFactory();

        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setFileSizeMax(MAX_FILE_SIZE);
        upload.setSizeMax(MAX_REQUEST_SIZE);
        upload.setHeaderEncoding("UTF-8");

        List<FileItem> items = null;
        try {
            items = upload.parseRequest(request);
            Iterator<FileItem> iterator = items.iterator();
            while (iterator.hasNext()) {
                FileItem item = iterator.next();
                if (item.isFormField()) {
                    String name = item.getFieldName();
                    String value = item.getString("UTF-8");
                    formFields.put(name, value);
                    iterator.remove();
                }
            }
        } catch (Exception ex) {
            log.error(ex);
            return;
        }
        Contact contact = null;
        Contact savedContact = null;
        try {
            contact = createContact();
            savedContact = ModificationContactService.addNewContact(contact);
        } catch (ParseException | ControllerException e) {
            log.error(e);
            request.setAttribute("invalidInput", "Invalid input.");
            try {
                request.getServletContext().getRequestDispatcher("/WEB-INF/pages/newContact.jsp").forward(request, response);
            } catch (ServletException | IOException e1) {
                log.error(e1);
            }
        } catch (ServiceException e) {
            log.error(e);
            request.setAttribute("error", "Sorry, contact hasn't been saved.");
            try {
                request.getServletContext().getRequestDispatcher("/WEB-INF/pages/newContact.jsp").forward(request, response);
            } catch (ServletException | IOException e1) {
                log.error(e1);
            }
        }

        String attachmentsDirectory = request.getServletContext().getInitParameter("attachmentsDirectory");
        ensureDirectoryExists(attachmentsDirectory);
        String imagesDirectory = request.getServletContext().getInitParameter("imagesDirectory");
        ensureDirectoryExists(imagesDirectory);

        try {
            Iterator<FileItem> iterator = items.iterator();
            while (iterator.hasNext()) {
                FileItem item = iterator.next();
                String realFileName = item.getName();
                String fieldName = item.getFieldName();
                String filePath = null;
                if ("userImage".equals(item.getFieldName())) {
                    filePath = imagesDirectory + File.separator + savedContact.getIdContact() + realFileName.substring(realFileName.lastIndexOf("."));
                } else {
                    Long attachmentId = null;
                    List<ContactAttachment> savedAttachments = savedContact.getAttachmentList();
                    String fileIndex = fieldName.substring(4);                          // "file{fileIndex}"
                    String fileName = formFields.get("fileName" + fileIndex);
                    for (ContactAttachment attachment : savedAttachments) {
                        if (attachment.getFileName().equals(fileName)) {
                            attachmentId = attachment.getIdAttachment();
                            attachment.setRealFileName(realFileName);
                        }
                    }
                    filePath = attachmentsDirectory + File.separator + attachmentId + "_" + realFileName;
                }
                File uploadedFile = new File(filePath);
                item.write(uploadedFile);
            }
            ModificationContactService.updateContactAttachments(savedContact);
            String path = request.getContextPath() + "/pages/contacts";
            response.sendRedirect(path);
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    private void ensureDirectoryExists(String directory) {
        File uploadDir = new File(directory);
        if (!uploadDir.exists()) {
            uploadDir.mkdir();
        }
    }

    private Contact createContact() throws ParseException, ControllerException {
        Contact contact = new Contact();
        setPersonalData(contact);
        setAddressToContact(contact);
        setPhonesToContact(contact);
        setAttachmentsToContact(contact);
        return contact;
    }

    private void checkCorrectness(String inputName) throws ControllerException {
        if (StringUtils.isEmpty(formFields.get(inputName)))
            throw new ControllerException("Invalid input.");
    }

    private void setPersonalData(Contact contact) throws ParseException, ControllerException {
        checkCorrectness("name");
        contact.setName(formFields.get("name"));
        checkCorrectness("surname");
        contact.setSurname(formFields.get("surname"));
        contact.setPatronymic(formFields.get("patronymic"));
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        Date parsed = format.parse(formFields.get("birthday"));
        contact.setBirthday(new Date(parsed.getTime()));
        String gender = formFields.get("gender");
        for (Gender genderValue : Gender.values()) {
            if (genderValue.name().equals(gender))
                contact.setGender(Gender.valueOf(gender));
        }
        contact.setCitizenship(formFields.get("citizenship"));
        contact.setWebsite(formFields.get("website"));
        checkCorrectness("email");
        contact.setEmail(formFields.get("email"));
        checkCorrectness("company");
        contact.setCompany(formFields.get("company"));
        String maritalStatus = formFields.get("marital");
        for (MaritalStatus status : MaritalStatus.values()) {
            if (status.name().equals(maritalStatus)) {
                contact.setMaritalStatus(MaritalStatus.valueOf(maritalStatus));
            }
        }
    }

    private void setAddressToContact(Contact contact) throws ControllerException {
        Address address = new Address();
        if ("NONE".equals(formFields.get("country")))
            throw new ControllerException("Invalid input.");
        address.setCountry(formFields.get("country"));
        checkCorrectness("city");
        address.setCity(formFields.get("city"));
        checkCorrectness("street");
        address.setStreet(formFields.get("street"));
        checkCorrectness("houseNumber");
        address.setHouseNumber(formFields.get("houseNumber"));
        address.setApartmentNumber(formFields.get("apartmentNumber"));
        address.setZipCode(formFields.get("zipCode"));
        contact.setAddress(address);
    }

    private void setPhonesToContact(Contact contact) throws ControllerException {
        List<ContactPhone> contactPhones = new ArrayList<>();
        int i = 0;
        while (formFields.get("countryCode" + i) != null) {
            ContactPhone phone = new ContactPhone();
            phone.setCountryCode(Integer.parseInt(formFields.get("countryCode" + i)));
            phone.setOperatorCode(Integer.parseInt(formFields.get("operatorCode" + i)));
            checkCorrectness("operatorCode" + i);
            phone.setPhoneNumber(Integer.parseInt(formFields.get("phoneNumber" + i)));
            String phoneType = formFields.get("phoneType" + i);
            for (PhoneType phoneTypeValue : PhoneType.values()) {
                if (phoneTypeValue.name().equals(phoneType)) {
                    phone.setPhoneType(PhoneType.valueOf(phoneType));
                }
            }
            phone.setComment(formFields.get("phoneComment" + i));
            phone.setIdPhone(contact.getIdContact());
            contactPhones.add(phone);
            i++;
        }
        contact.setPhoneList(contactPhones);
    }

    private void setAttachmentsToContact(Contact contact) throws ControllerException {
        List<ContactAttachment> contactAttachments = new ArrayList<>();
        int i = 0;
        while (formFields.get("fileName" + i) != null) {
            ContactAttachment attachment = new ContactAttachment();
            checkCorrectness("fileName" + i);
            attachment.setFileName(formFields.get("fileName" + i));
            checkCorrectness("attachingDate" + i);
            attachment.setUploadDate(new Date(Long.parseLong(formFields.get("attachingDate" + i))));
            attachment.setComment(formFields.get("attachmentComment" + i));
            attachment.setIdAttachment(contact.getIdContact());
            contactAttachments.add(attachment);
            i++;
        }
        contact.setAttachmentList(contactAttachments);
    }
}
