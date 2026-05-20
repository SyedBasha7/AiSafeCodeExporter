package com.inneo.aisafecodesync.web.validation;

import com.inneo.aisafecodesync.web.dto.ProfileForm;
import com.inneo.aisafecodesync.web.mapper.ProfileMapper;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class ProfileFormValidator implements Validator {

    private final ProfileMapper profileMapper;

    public ProfileFormValidator(ProfileMapper profileMapper) {
        this.profileMapper = profileMapper;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return ProfileForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        try {
            profileMapper.toConfig((ProfileForm) target);
        } catch (RuntimeException ex) {
            errors.reject("profile.invalid", ex.getMessage());
        }
    }
}
