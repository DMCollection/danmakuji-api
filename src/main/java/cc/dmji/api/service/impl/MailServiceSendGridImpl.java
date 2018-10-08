/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package cc.dmji.api.service.impl;

import cc.dmji.api.service.MailService;
import com.sendgrid.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.io.IOException;

@Service
public class MailServiceSendGridImpl implements MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailServiceImpl.class);

    private static final String MAIL_NAME = "Darker";

    private static final String MAIL_CONTENT = "<html><body><a href='https://darker.me/#/vemail?userId=%s&uuid=%s' target='_blank'>单击此处完成注册</a><br><p>有效期20分钟</p></body></html>";


    @Value("${spring.mail.username}")
    private String from;

    @Autowired
    private SendGrid sendGrid;

    @Override
    @Async
    public void sendVerifyEmail(String toEmail, Long userId, String uuid) throws MessagingException {
        Email from = new Email(this.from, MAIL_NAME);
        String subject = "Welcome to become a Darker!";
        Email to = new Email(toEmail);
        String href = String.format(MAIL_CONTENT, userId, uuid);
        Content content = new Content("text/html",href);
        Mail mail = new Mail(from,subject,to,content);
        Request request = new Request();
        try{
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            logger.debug("发送的邮件地址:{}", href);
            logger.debug("SendGrid StatusCode:{}",response.getStatusCode());
            logger.debug("SendGrid Response Body:{}",response.getBody());
            logger.debug("SendGrid Headers:{}",response.getHeaders());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    @Async
    public void sendVerifyCodeEmail(String toEmail, String content) throws MessagingException {
        Email from = new Email(this.from, MAIL_NAME);
        String subject = "请查收验证码!";
        Email to = new Email(toEmail);
        Content mailContent = new Content("text/html",content);
        Mail mail = new Mail(from,subject,to,mailContent);
        Request request = new Request();
        try{
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            logger.debug("发送的邮件地址:{}，验证码:{}", toEmail, content);
            logger.debug("SendGrid StatusCode:{}",response.getStatusCode());
            logger.debug("SendGrid Response Body:{}",response.getBody());
            logger.debug("SendGrid Headers:{}",response.getHeaders());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendEmail(String toEmail, String title, String content) throws MessagingException {
        Email from = new Email(this.from, MAIL_NAME);
        Email to = new Email(toEmail);
        Content mailContent = new Content("text/html",content);
        Mail mail = new Mail(from,title,to,mailContent);
        Request request = new Request();
        try{
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            logger.debug("发送的邮件地址:{},标题:{},正文:{}", toEmail, title, content);
            logger.debug("SendGrid StatusCode:{}",response.getStatusCode());
            logger.debug("SendGrid Response Body:{}",response.getBody());
            logger.debug("SendGrid Headers:{}",response.getHeaders());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
