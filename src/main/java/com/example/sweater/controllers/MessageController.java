package com.example.sweater.controllers;

import com.example.sweater.domain.Message;
import com.example.sweater.domain.User;
import com.example.sweater.repos.MessageRepo;
import com.example.sweater.repos.UserRepo;
import com.example.sweater.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;

@Controller
public class MessageController {
    @Autowired
    private MessageRepo messageRepo;
    @Autowired
    private UserRepo userRepo;
    @GetMapping("user/messages/{user}")
    public String getMessageList(@AuthenticationPrincipal User currentUser,
                                 @PathVariable User user,
                                 @RequestParam(required = false) Message message,
                                 Model model) {
        currentUser=userRepo.findByUsername(currentUser.getUsername());
        if (message != null) {
            model.addAttribute(message);
        }


        model.addAttribute("podpisan", user.getSubscribers().contains(currentUser));
        model.addAttribute("nepodpisan", !user.getSubscribers().contains(currentUser));
        model.addAttribute("user", user);
        model.addAttribute("countSubscriptions", user.getSubscriptions().size());
        model.addAttribute("countSubscribers", user.getSubscribers().size());
        model.addAttribute("currUser", user.getId().equals(currentUser.getId()));
        model.addAttribute("notYourProfile", !user.getId().equals(currentUser.getId()));
        model.addAttribute("messages", user.getMessages());
        return "usermessages";
    }

    @PostMapping("user/messages/{user}")
    public String editMessageFromMessageList(@AuthenticationPrincipal User currentUser,
                                             @PathVariable Long user,
                                             @RequestParam("id") Message oldMessage,
                                             @RequestParam("text") String newText,
                                             @RequestParam(value = "tag", required = false) String newTag,
                                             @RequestParam("button") String valueKnopka,
                                             @RequestParam(value = "file") MultipartFile file,
                                             Model model) throws IOException {

        if (valueKnopka.equals("delete")) {
            System.out.println("Будет удаление");
            messageRepo.delete(oldMessage);
            return "redirect:/user/messages/{user}";
        }
        if (oldMessage.getAuthor().getId().equals(currentUser.getId())) {
            if (!StringUtils.isEmpty(newText)) {
                oldMessage.setText(newText);
            }
            if (!StringUtils.isEmpty(newTag)) {
                oldMessage.setTag(newTag);
            }

            if (file != null && !file.getOriginalFilename().isEmpty()) {
                FileService.uploadAndSaveFile(oldMessage, file);
            } else {
                oldMessage.setFilename(null);
            }
            messageRepo.save(oldMessage);
        }

        return "redirect:/user/messages/{user}";
    }

    @GetMapping("/user/{user}/{mode}")
    public String getSub(Model model, @PathVariable User user, @PathVariable String mode) {
        model.addAttribute("mode", mode);
        model.addAttribute("user", user);
        if (mode.equals("subscribers")) {
            model.addAttribute("users", user.getSubscribers());
        } else {
            model.addAttribute("users", user.getSubscriptions());
        }
        return "subslist";
    }
}
