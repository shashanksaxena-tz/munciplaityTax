package com.munitax.submission.service;

import com.munitax.submission.model.Submission;
import com.munitax.submission.model.DocumentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service for sending email notifications for audit workflow events.
 * 
 * NOTE: This is a placeholder implementation that logs email content.
 * In production, integrate with actual email service (e.g., SendGrid, AWS SES, SMTP).
 * 
 * Implements FR-042 to FR-046: Communication & Messaging
 */
@Service
@Slf4j
public class EmailNotificationService {
    
    private static final String FROM_EMAIL = "noreply@dublin-tax.gov";
    private static final String FROM_NAME = "Dublin Municipality Tax Department";
    
    /**
     * Send approval notification to taxpayer
     */
    public void sendApprovalNotification(Submission submission, String auditorName, String paymentDueDate) {
        String to = getSubmissionEmail(submission);
        String subject = "Tax Return Approved - " + submission.getTaxYear();
        
        String body = buildApprovalEmail(submission, auditorName, paymentDueDate);
        
        sendEmail(to, subject, body, "APPROVAL");
    }
    
    /**
     * Send rejection notification to taxpayer
     */
    public void sendRejectionNotification(Submission submission, String reason, 
                                         String detailedExplanation, LocalDate resubmitDeadline) {
        String to = getSubmissionEmail(submission);
        String subject = "Tax Return Requires Revision - " + submission.getTaxYear();
        
        String body = buildRejectionEmail(submission, reason, detailedExplanation, resubmitDeadline);
        
        sendEmail(to, subject, body, "REJECTION");
    }
    
    /**
     * Send document request notification to taxpayer
     */
    public void sendDocumentRequestNotification(Submission submission, DocumentRequest request) {
        String to = getSubmissionEmail(submission);
        String subject = "Additional Documentation Required - " + submission.getTaxYear();
        
        String body = buildDocumentRequestEmail(submission, request);
        
        sendEmail(to, subject, body, "DOC_REQUEST");
    }
    
    /**
     * Send reminder notification before document deadline
     */
    public void sendDocumentRequestReminder(Submission submission, DocumentRequest request, int daysUntilDeadline) {
        String to = getSubmissionEmail(submission);
        String subject = String.format("Reminder: Document Submission Due in %d Day%s", 
                daysUntilDeadline, daysUntilDeadline == 1 ? "" : "s");
        
        String body = buildReminderEmail(submission, request, daysUntilDeadline);
        
        sendEmail(to, subject, body, "REMINDER");
    }
    
    /**
     * Send payment reminder notification
     */
    public void sendPaymentReminder(Submission submission, LocalDate paymentDueDate) {
        String to = getSubmissionEmail(submission);
        String subject = "Payment Reminder - Tax Payment Due";
        
        String body = buildPaymentReminderEmail(submission, paymentDueDate);
        
        sendEmail(to, subject, body, "PAYMENT_REMINDER");
    }
    
    /**
     * Send daily queue summary to auditors
     */
    public void sendDailyQueueSummary(String auditorEmail, int pendingCount, 
                                     int highPriorityCount, int assignedCount) {
        String subject = "Daily Audit Queue Summary";
        
        String body = String.format("""
            Dear Auditor,
            
            Here is your daily audit queue summary:
            
            - Pending Returns: %d
            - High Priority Returns: %d
            - Assigned to You: %d
            
            Please log in to the auditor portal to review and process pending returns.
            
            Dublin Municipality Tax Department
            """, pendingCount, highPriorityCount, assignedCount);
        
        sendEmail(auditorEmail, subject, body, "QUEUE_SUMMARY");
    }
    
    // ===== Email Template Builders =====
    
    private String buildApprovalEmail(Submission submission, String auditorName, String paymentDueDate) {
        double taxDue = submission.getTaxDue() != null ? submission.getTaxDue() : 0.0;
        
        return String.format("""
            Dear %s,
            
            Your %d tax return has been approved by our office.
            
            Return Details:
            - Tax Year: %d
            - Return Type: %s
            - Tax Due: $%.2f
            - Approved By: %s
            
            %s
            
            If you have any questions, please contact the Dublin Tax Department at (555) 123-4567.
            
            Thank you for your timely filing.
            
            Sincerely,
            Dublin Municipality Tax Department
            """, 
            submission.getTaxpayerName() != null ? submission.getTaxpayerName() : "Taxpayer",
            submission.getTaxYear(),
            submission.getTaxYear(),
            submission.getReturnType(),
            taxDue,
            auditorName,
            taxDue > 0 ? 
                String.format("Payment Due: %s\nPlease remit payment by the due date to avoid penalties and interest.", paymentDueDate) :
                "No payment is due at this time."
        );
    }
    
    private String buildRejectionEmail(Submission submission, String reason, 
                                      String detailedExplanation, LocalDate resubmitDeadline) {
        return String.format("""
            Dear %s,
            
            Your %d tax return requires revision before it can be approved.
            
            Reason for Rejection: %s
            
            Details:
            %s
            
            Please make the necessary corrections and resubmit your return by %s.
            
            If you do not resubmit by the deadline, your return will be considered delinquent 
            and penalties may apply.
            
            If you have questions about the required corrections, please contact us at (555) 123-4567.
            
            Sincerely,
            Dublin Municipality Tax Department
            """,
            submission.getTaxpayerName() != null ? submission.getTaxpayerName() : "Taxpayer",
            submission.getTaxYear(),
            formatRejectionReason(reason),
            detailedExplanation,
            resubmitDeadline.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        );
    }
    
    private String buildDocumentRequestEmail(Submission submission, DocumentRequest request) {
        return String.format("""
            Dear %s,
            
            We require additional documentation to complete the review of your %d tax return.
            
            Document Type Requested: %s
            
            Details:
            %s
            
            Please submit the requested documents by %s.
            
            You can upload documents through our online portal at:
            https://dublin-tax.gov/upload/%s
            
            If you have questions, please contact us at (555) 123-4567.
            
            Sincerely,
            Dublin Municipality Tax Department
            """,
            submission.getTaxpayerName() != null ? submission.getTaxpayerName() : "Taxpayer",
            submission.getTaxYear(),
            formatDocumentType(request.getDocumentType().name()),
            request.getDescription(),
            request.getDeadline().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
            request.getRequestId()
        );
    }
    
    private String buildReminderEmail(Submission submission, DocumentRequest request, int daysUntilDeadline) {
        return String.format("""
            Dear %s,
            
            This is a reminder that we are still awaiting the following documentation for your %d tax return:
            
            Document Type: %s
            Description: %s
            Deadline: %s (%d day%s remaining)
            
            Please submit the requested documents as soon as possible to avoid delays in processing your return.
            
            Upload documents at: https://dublin-tax.gov/upload/%s
            
            Sincerely,
            Dublin Municipality Tax Department
            """,
            submission.getTaxpayerName() != null ? submission.getTaxpayerName() : "Taxpayer",
            submission.getTaxYear(),
            formatDocumentType(request.getDocumentType().name()),
            request.getDescription(),
            request.getDeadline().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
            daysUntilDeadline,
            daysUntilDeadline == 1 ? "" : "s",
            request.getRequestId()
        );
    }
    
    private String buildPaymentReminderEmail(Submission submission, LocalDate paymentDueDate) {
        double taxDue = submission.getTaxDue() != null ? submission.getTaxDue() : 0.0;
        
        return String.format("""
            Dear %s,
            
            This is a reminder that your %d tax payment is due.
            
            Amount Due: $%.2f
            Payment Due Date: %s
            
            Please remit payment promptly to avoid late payment penalties and interest charges.
            
            You can make a payment online at: https://dublin-tax.gov/payment
            
            Thank you,
            Dublin Municipality Tax Department
            """,
            submission.getTaxpayerName() != null ? submission.getTaxpayerName() : "Taxpayer",
            submission.getTaxYear(),
            taxDue,
            paymentDueDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        );
    }
    
    // ===== Helper Methods =====
    
    private void sendEmail(String to, String subject, String body, String emailType) {
        // Skip if no email address configured
        if (to == null || to.isEmpty()) {
            log.info("Skipping email send (no recipient configured) - Type: {}, Subject: {}", emailType, subject);
            return;
        }
        
        // TODO: Integrate with actual email service
        // For now, log the email for demonstration
        log.info(String.format("""
            ========================================
            EMAIL NOTIFICATION [%s]
            ========================================
            From: %s <%s>
            To: %s
            Subject: %s
            
            Body:
            %s
            ========================================
            """, emailType, FROM_NAME, FROM_EMAIL, to, subject, body));
        
        // In production, implement actual email sending:
        // emailClient.send(to, subject, body);
    }
    
    private String getSubmissionEmail(Submission submission) {
        // TODO: Retrieve actual email from user/taxpayer record
        // In development/test, return a configurable test email
        // In production, this should query the user/taxpayer database
        
        String testEmail = System.getenv("AUDIT_TEST_EMAIL");
        if (testEmail != null && !testEmail.isEmpty()) {
            log.info("Using test email address: {}", testEmail);
            return testEmail;
        }
        
        // Fallback to logging-only mode (email service not fully configured)
        log.warn("Email service not configured. Set AUDIT_TEST_EMAIL environment variable or implement production email lookup.");
        return null; // Will skip actual email sending but continue workflow
    }
    
    private String formatRejectionReason(String reason) {
        return reason.replace("_", " ");
    }
    
    private String formatDocumentType(String docType) {
        return docType.replace("_", " ");
    }
}
