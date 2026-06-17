package mecklon.scribbleIoClone.service;

import lombok.RequiredArgsConstructor;
import mecklon.scribbleIoClone.dto.ProfileDTO;
import mecklon.scribbleIoClone.model.User;
import mecklon.scribbleIoClone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public ProfileDTO updateProfile(MultipartFile profile, String username, Authentication auth) throws IOException {
        try{
            UserDetails userDetails = (UserDetails)auth.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername());
            if(profile!=null) {
                String uniqueName = System.currentTimeMillis() + "-" + profile.getOriginalFilename();
                String path = uploadDir + File.separator + uniqueName;
                profile.transferTo(new File(path));
                user.setFilePath(path);
                user.setFileName(uniqueName);
            }
            user.setUsername(username);
            User newUser =  userRepository.save(user);
            return new ProfileDTO(newUser.getUsername(), newUser.getFileName());
        }catch(Exception e){
            e.printStackTrace();
            throw e;
        }
    }
}
