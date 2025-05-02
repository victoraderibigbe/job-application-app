package com.bytesnova.jobs.controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.bytesnova.jobs.models.Applicant;
import com.bytesnova.jobs.repositories.ApplicantRepository;

@Controller
public class JobFormController {

    @Autowired
    private ApplicantRepository applicantRepository;
    private JavaMailSender mailSender;

    @GetMapping("/job-form")
    public String jobForm(Model model) {
        Applicant applicant = new Applicant();
        model.addAttribute("applicant", applicant);
        return "job_application";
    }

    @PostMapping("/submit-application")
    public String submitApplication(@ModelAttribute("applicant") Applicant applicant, Model model) {
        // Save applicant details to the database
        applicantRepository.save(applicant);

        String filepath = "src/main/resources/static/applicant_details.txt";

        // Write applicant details to a text file
        StringBuilder fileContent = new StringBuilder();
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(filepath, true))) {
            writer.write("Dear " + applicant.getFirstName() + ",");
            writer.newLine();
            writer.newLine();
            fileContent.append("Dear ").append(applicant.getFirstName()).append(",\n\n");
            writer.write("Thank you for applying for the job. Here are your details:");
            writer.newLine();
            writer.newLine();
            fileContent.append("Thank you for applying for the job. Here are your details:\n\n");

            writer.write("First Name: " + applicant.getFirstName());
            writer.newLine();
            fileContent.append("Name: ").append(applicant.getFirstName()).append("\n");

            writer.write("Last Name: " + applicant.getLastName());
            writer.newLine();
            fileContent.append("Last Name: ").append(applicant.getLastName()).append("\n");

            writer.write("Email: " + applicant.getEmail());
            writer.newLine();
            fileContent.append("Email: ").append(applicant.getEmail()).append("\n");

            writer.write("Phone Number: " + applicant.getPhoneNumber());
            writer.newLine();
            fileContent.append("Phone Number: ").append(applicant.getPhoneNumber()).append("\n");

            writer.write("Address: " + applicant.getAddress());
            writer.newLine();
            fileContent.append("Address: ").append(applicant.getAddress()).append("\n");

            writer.write("Profession: " + applicant.getProfession());
            writer.newLine();
            fileContent.append("Profession: ").append(applicant.getProfession()).append("\n");

            writer.write("Hobby: " + applicant.getHobby());
            writer.newLine();
            fileContent.append("Hobby: ").append(applicant.getHobby()).append("\n");

            writer.write("Age: " + applicant.getAge());
            writer.newLine();
            fileContent.append("Age: ").append(applicant.getAge()).append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Send email with the applicant details and attach the text file
        sendEmailWithAttachment(applicant.getEmail(), fileContent.toString(), filepath);

        // Redirect to the results page
        return "redirect:/applicants";
    }

    @GetMapping("/download-file")
    public ResponseEntity<Resource> downloadFile() {
        String filePath = "src/main/resources/static/applicant_details.txt";
        File file = new File(filePath);

        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        Resource resource = new FileSystemResource(file);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @GetMapping("/results")
    public String showResults(Model model) {
        // Read the contents of the text file
        List<String> fileContents = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new FileReader("src/main/resources/static/applicant_details.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContents.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Add the file contents to the model
        model.addAttribute("fileContents", fileContents);

        return "results";
    }

    @GetMapping("/applicants")
    public String listApplicants(Model model) {
        // Fetch all applicants from the database
        List<Applicant> applicants = applicantRepository.findAll();
        model.addAttribute("applicants", applicants);
        return "applicants";
    }

    @GetMapping("/applicants/view/{id}")
    public String viewApplicant(@PathVariable Long id, Model model) {
        Applicant applicant = applicantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid applicant ID: " + id));
        model.addAttribute("applicant", applicant);
        return "view_applicant";
    }

    @GetMapping("/applicants/edit/{id}")
    public String editApplicant(@PathVariable Long id, Model model) {
        Applicant applicant = applicantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid applicant ID: " + id));
        model.addAttribute("applicant", applicant);
        return "edit_applicant";
    }

    @PostMapping("/applicants/edit/{id}")
    public String updateApplicant(@PathVariable Long id, @ModelAttribute Applicant applicant) {
        applicant.setId(id);
        applicantRepository.save(applicant);
        return "redirect:/applicants";
    }

    @GetMapping("/applicants/delete/{id}")
    public String deleteApplicant(@PathVariable Long id) {
        applicantRepository.deleteById(id);
        return "redirect:/applicants";
    }

    private void sendEmailWithAttachment(String recipientEmail, String messageContent, String filePath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(recipientEmail);
            message.setSubject("Your Job Application Details");
            helper.setText(messageContent);

            File file = new File(filePath);

            if (file.exists()) {
                helper.addAttachment(file.getName(), file);
            } else {
                System.err.println("File not found: " + filePath);
            }

            mailSender.send(message);
            System.out.println("Email sent successfully to " + recipientEmail);
        } catch (MessagingException me) {
            System.err.println("Error sending email: " + me.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
