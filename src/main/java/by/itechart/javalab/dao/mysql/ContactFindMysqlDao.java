package by.itechart.javalab.dao.mysql;


import by.itechart.javalab.dao.ContactFindDao;
import by.itechart.javalab.dao.DaoException;
import by.itechart.javalab.entity.*;
import by.itechart.javalab.persistence.PersistenceManager;
import by.itechart.javalab.service.ContactSearchAttributes;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.NamingException;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public final class ContactFindMysqlDao implements ContactFindDao {
    private final static ContactFindMysqlDao instance = new ContactFindMysqlDao();
    private static Logger log = LogManager.getLogger(ContactFindMysqlDao.class.getName());
    private final int MAX_CONTACTS_NUMBER = 11;

    private ContactFindMysqlDao() {}

    public static ContactFindMysqlDao getInstance(){
        return instance;
    }

    @Override
    public List<Contact> getContacts(Integer offset, boolean isLowerIds) throws DaoException {
        log.debug("getContacts: " + offset);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Contact> contacts = new ArrayList<>();
        try {
            connection = PersistenceManager.createConnection();
            StringBuilder preparedStatementSQL = new StringBuilder("select idContact, name, surname, birthday, " +
                    " company, city, street, houseNumber, apartmentNumber from contact where ");
            if (isLowerIds)
                preparedStatementSQL.append("idContact < ? AND available = ? ORDER BY idContact DESC LIMIT ?");
            else
                preparedStatementSQL.append("idContact > ? AND available = ? ORDER BY idContact ASC LIMIT ?");
            statement = connection.prepareStatement(preparedStatementSQL.toString());
            statement.setInt(1, offset);
            statement.setBoolean(2, true);
            statement.setInt(3, MAX_CONTACTS_NUMBER);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Contact contact = new Contact();
                contact.setIdContact(resultSet.getLong("idContact"));
                contact.setName(resultSet.getString("name"));
                contact.setSurname(resultSet.getString("surname"));
                contact.setBirthday(resultSet.getDate("birthday"));
                contact.setCompany(resultSet.getString("company"));
                Address address = new Address();
                address.setCity(resultSet.getString("city"));
                address.setStreet(resultSet.getString("street"));
                address.setHouseNumber(resultSet.getString("houseNumber"));
                address.setApartmentNumber(resultSet.getString("apartmentNumber"));
                contact.setAddress(address);
                contacts.add(contact);
            }
        } catch (NamingException | SQLException ex) {
            log.error(ex);
            throw new DaoException("Can't get contacts.", ex);
        } finally {
            closeStatement(statement);
            PersistenceManager.closeConnection(connection);
        }
        if (isLowerIds)
            Collections.reverse(contacts);
        return contacts;
    }

    @Override
    public List<Contact> getContacts(ContactSearchAttributes searchAttributes, Integer offset, boolean isLowerIds) throws DaoException {
        log.debug("getContacts: " + offset);
        log.debug("searchAttributes: {}", searchAttributes.getName());
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Contact> contacts = new ArrayList<>();
        try {
            connection = PersistenceManager.createConnection();
            StringBuilder preparedStatementSQL = new StringBuilder("select idContact, name, surname, birthday, " +
                    " company, city, street, houseNumber, apartmentNumber from contact where " +
                    "available = ? ");
            Map<Integer, Object> preparedStatementParameters = new HashMap<>();
            int parameterPosition = 2;
            if (StringUtils.isNotEmpty(searchAttributes.getName())) {
                preparedStatementSQL.append("AND name = ? ");
                preparedStatementParameters.put(parameterPosition, searchAttributes.getName());
                parameterPosition++;
            }
            if (StringUtils.isNotEmpty(searchAttributes.getSurname())) {
                preparedStatementSQL.append("AND surname = ? ");
                preparedStatementParameters.put(parameterPosition, searchAttributes.getSurname());
                parameterPosition++;
            }
            if (StringUtils.isNotEmpty(searchAttributes.getPatronymic())) {
                preparedStatementSQL.append("AND patronymic = ? ");
                preparedStatementParameters.put(parameterPosition, searchAttributes.getPatronymic());
                parameterPosition++;
            }
            if (StringUtils.isNotEmpty(searchAttributes.getCitizenship())) {
                preparedStatementSQL.append("AND citizenship = ? ");
                preparedStatementParameters.put(parameterPosition, searchAttributes.getCitizenship());
                parameterPosition++;
            }
            if (searchAttributes.getGender() != null) {
                preparedStatementSQL.append("AND gender = ? ");
                preparedStatementParameters.put(parameterPosition, searchAttributes.getGender().name());
                parameterPosition++;
            }
            if (searchAttributes.getMaritalStatus() != null) {
                preparedStatementSQL.append("AND maritalStatus = ? ");
                preparedStatementParameters.put(parameterPosition, searchAttributes.getMaritalStatus().name());
                parameterPosition++;
            }
            if (searchAttributes.getBirthdayDateFrom() != null) {
                preparedStatementSQL.append("AND birthday >= ? ");
                preparedStatementParameters.put(parameterPosition, new Date(searchAttributes.getBirthdayDateFrom().getTime()));
                parameterPosition++;
            }
            if (searchAttributes.getBirthdayDateTo() != null) {
                preparedStatementSQL.append("AND birthday <= ? ");
                preparedStatementParameters.put(parameterPosition, new Date(searchAttributes.getBirthdayDateTo().getTime()));
                parameterPosition++;
            }
            if (StringUtils.isNotEmpty(searchAttributes.getAddress().getCountry())) {
                preparedStatementSQL.append("AND Country_idCountryCode = (SELECT idCountryCode from country where fullName = ?) ");
                preparedStatementParameters.put(parameterPosition, searchAttributes.getAddress().getCountry());
                parameterPosition++;
            }
            if (StringUtils.isNotEmpty(searchAttributes.getAddress().getCity())) {
                preparedStatementSQL.append("AND city = ? ");
                preparedStatementParameters.put(parameterPosition, searchAttributes.getAddress().getCity());
                parameterPosition++;
            }
            if (StringUtils.isNotEmpty(searchAttributes.getAddress().getStreet())) {
                preparedStatementSQL.append("AND street = ? ");
                preparedStatementParameters.put(parameterPosition, searchAttributes.getAddress().getStreet());
                parameterPosition++;
            }
            if (StringUtils.isNotEmpty(searchAttributes.getAddress().getHouseNumber())) {
                preparedStatementSQL.append("AND houseNumber = ? ");
                preparedStatementParameters.put(parameterPosition, searchAttributes.getAddress().getHouseNumber());
                parameterPosition++;
            }
            if (StringUtils.isNotEmpty(searchAttributes.getAddress().getApartmentNumber())) {
                preparedStatementSQL.append("AND apartmentNumber = ? ");
                preparedStatementParameters.put(parameterPosition, searchAttributes.getAddress().getApartmentNumber());
                parameterPosition++;
            }
            if (searchAttributes.getAddress().getZipCode() != null) {
                preparedStatementSQL.append("AND zipCode = ? ");
                preparedStatementParameters.put(parameterPosition, searchAttributes.getAddress().getZipCode());
                parameterPosition++;
            }
            if (isLowerIds)
                preparedStatementSQL.append("AND idContact < ? ORDER BY idContact DESC LIMIT ?");
            else
                preparedStatementSQL.append("AND idContact > ? ORDER BY idContact ASC LIMIT ?");
            statement = connection.prepareStatement(preparedStatementSQL.toString());
            statement.setBoolean(1, true);
            statement.setInt(parameterPosition, offset);
            statement.setInt(++parameterPosition, MAX_CONTACTS_NUMBER);
            for (Integer position : preparedStatementParameters.keySet()) {
                if (preparedStatementParameters.get(position) instanceof String) {
                    statement.setString(position, (String) preparedStatementParameters.get(position));
                    continue;
                }
                if (preparedStatementParameters.get(position) instanceof Integer) {
                    statement.setInt(position, (Integer) preparedStatementParameters.get(position));
                    continue;
                }
                if (preparedStatementParameters.get(position) instanceof Date) {
                    statement.setDate(position, (Date) preparedStatementParameters.get(position));
                }
            }
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Contact contact = new Contact();
                contact.setIdContact(resultSet.getLong("idContact"));
                contact.setName(resultSet.getString("name"));
                contact.setSurname(resultSet.getString("surname"));
                contact.setBirthday(resultSet.getDate("birthday"));
                contact.setCompany(resultSet.getString("company"));
                Address address = new Address();
                address.setCity(resultSet.getString("city"));
                address.setStreet(resultSet.getString("street"));
                address.setHouseNumber(resultSet.getString("houseNumber"));
                address.setApartmentNumber(resultSet.getString("apartmentNumber"));
                contact.setAddress(address);
                contacts.add(contact);
            }
        } catch (NamingException | SQLException ex) {
            log.error(ex);
            throw new DaoException("Can't get contacts.", ex);
        } finally {
            closeStatement(statement);
            PersistenceManager.closeConnection(connection);
        }
        if (isLowerIds)
            Collections.reverse(contacts);
        return contacts;
    }

    @Override
    public Contact getContact(Integer contactId) throws DaoException {
        log.debug("getContact: " + contactId);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Contact contact = null;
        try {
            connection = PersistenceManager.createConnection();
            statement = connection.prepareStatement("SELECT contact.idContact, contact.name, " +
                    "contact.surname, contact.patronymic, contact.birthday, contact.gender, " +
                    "contact.citizenship, contact.website, contact.email, contact.company, contact.maritalStatus, " +
                    "country.fullName, contact.city, contact.street, contact.houseNumber, contact.apartmentNumber, contact.zipCode, " +
                    "phone.idPhone, phone.countryCode, phone.operatorCode, phone.phoneNumber, phone.phoneType, phone.comment, " +
                    "attachment.idAttachment, attachment.fileName, attachment.uploadDate, attachment.comment, attachment.realFileName " +
                    "FROM contact " +
                    "JOIN country " +
                    "ON contact.Country_idCountryCode = country.idCountryCode " +
                    "JOIN phone " +
                    "on phone.Contact_idContact = contact.idContact " +
                    "JOIN attachment " +
                    "on attachment.Contact_idContact = contact.idContact " +
                    "WHERE contact.idContact = ? AND contact.available = ? and phone.available = ? and attachment.available = ?");
            statement.setInt(1, contactId);
            statement.setBoolean(2, true);
            statement.setBoolean(3, true);
            statement.setBoolean(4, true);
            resultSet = statement.executeQuery();
            contact = new Contact();
            Set<ContactPhone> phones = new HashSet<>();
            Set<ContactAttachment> attachments = new HashSet<>();
            if (resultSet.next()) {
                contact.setIdContact(resultSet.getLong("contact.idContact"));
                contact.setName(resultSet.getString("contact.name"));
                contact.setSurname(resultSet.getString("contact.surname"));
                contact.setPatronymic(resultSet.getString("contact.patronymic"));
                contact.setBirthday(resultSet.getDate("contact.birthday"));
                contact.setGender(Gender.valueOf(resultSet.getString("contact.gender")));
                contact.setCitizenship(resultSet.getString("contact.citizenship"));
                contact.setWebsite(resultSet.getString("contact.website"));
                contact.setEmail(resultSet.getString("contact.email"));
                contact.setCompany(resultSet.getString("contact.company"));
                contact.setMaritalStatus(MaritalStatus.valueOf(resultSet.getString("contact.maritalStatus")));
                Address address = new Address();
                address.setCountry(resultSet.getString("country.fullName"));
                address.setCity(resultSet.getString("contact.city"));
                address.setStreet(resultSet.getString("contact.street"));
                address.setHouseNumber(resultSet.getString("contact.houseNumber"));
                address.setApartmentNumber(resultSet.getString("contact.apartmentNumber"));
                address.setZipCode(resultSet.getInt("contact.zipCode"));
                contact.setAddress(address);
                ContactPhone phone = getContactPhone(resultSet);
                phones.add(phone);
                ContactAttachment attachment = getContactAttachment(resultSet);
                attachments.add(attachment);
            }
            while (resultSet.next()) {
                ContactPhone phone = getContactPhone(resultSet);
                phones.add(phone);
                ContactAttachment attachment = getContactAttachment(resultSet);
                attachments.add(attachment);
            }
            log.debug("phones: ");
            for (ContactPhone phone : phones) {
                log.debug(phone.getIdPhone() + " " + phone.getComment());
            }
            log.debug("attachments: ");
            for (ContactAttachment attachment : attachments) {
                log.debug(attachment.getIdAttachment() + " " + attachment.getFileName());
            }
            contact.setPhoneList(new ArrayList<ContactPhone>(phones));
            contact.setAttachmentList(new ArrayList<ContactAttachment>(attachments));
        } catch (NamingException | SQLException ex) {
            log.error(ex);
            throw new DaoException("Can't get contact.", ex);
        } finally {
            closeStatement(statement);
            PersistenceManager.closeConnection(connection);
        }
        return contact;
    }

    private ContactPhone getContactPhone(ResultSet resultSet) throws SQLException{
        ContactPhone phone = new ContactPhone();
        phone.setIdPhone(resultSet.getLong("phone.idPhone"));
        phone.setCountryCode(resultSet.getInt("phone.countryCode"));
        phone.setOperatorCode(resultSet.getInt("phone.operatorCode"));
        phone.setPhoneNumber(resultSet.getInt("phone.phoneNumber"));
        phone.setPhoneType(PhoneType.valueOf(resultSet.getString("phone.phoneType")));
        phone.setComment(resultSet.getString("phone.comment"));
        return phone;
    }

    private ContactAttachment getContactAttachment(ResultSet resultSet) throws SQLException {
        ContactAttachment attachment = new ContactAttachment();
        attachment.setIdAttachment(resultSet.getLong("attachment.idAttachment"));
        attachment.setFileName(resultSet.getString("attachment.fileName"));
        attachment.setUploadDate(resultSet.getDate("attachment.uploadDate"));
        attachment.setRealFileName(resultSet.getString("attachment.realFileName"));
        attachment.setComment(resultSet.getString("attachment.comment"));
        return attachment;
    }

    @Override
    public List<String> getEmails(Integer[] contactId) throws DaoException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<String> emails = new ArrayList<>();
        try {
            connection = PersistenceManager.createConnection();
            statement = connection.prepareStatement("SELECT email FROM contact WHERE idContact = ?");
            for (Integer id : contactId) {
                statement.setInt(1, id);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    emails.add(resultSet.getString("email"));
                }
            }
        } catch (NamingException | SQLException ex) {
            log.error(ex);
            throw new DaoException("Can't get emails.", ex);
        } finally {
            closeStatement(statement);
            PersistenceManager.closeConnection(connection);
        }
        return emails;
    }

    private void closeStatement(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ex) {
                log.error(ex);
            }
        }
    }
}
