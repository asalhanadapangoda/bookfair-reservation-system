import React from 'react';
import {
    Box,
    Button,
    TextField,
    Typography,
    Link,
    IconButton,
    InputAdornment,
    CircularProgress,
    Checkbox,
    FormControlLabel,
    useTheme,
    alpha,
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { Visibility, VisibilityOff, Email, Lock, Login as LoginIcon } from '@mui/icons-material';

const SignInForm = ({
    formData,
    handleChange,
    handleSubmit,
    errors,
    loading,
    showPassword,
    togglePasswordVisibility,
    rememberMe,
    setRememberMe,
    passwordHelperText
}) => {
    const theme = useTheme();
    const inputSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '14px',
            backgroundColor: alpha(theme.palette.common.white, 0.7),
            backdropFilter: 'blur(8px)',
            transition: 'all 0.2s ease',
            '& fieldset': {
                borderColor: alpha(theme.palette.primary.main, 0.18),
            },
            '&:hover': {
                backgroundColor: alpha(theme.palette.common.white, 0.9),
            },
            '&:hover fieldset': {
                borderColor: alpha(theme.palette.primary.main, 0.45),
            },
            '&.Mui-focused fieldset': {
                borderColor: theme.palette.primary.main,
                borderWidth: 2,
            },
        },
    };

    return (
        <Box component="form" noValidate onSubmit={handleSubmit} sx={{ mt: 1, width: '100%' }}>
            <Box sx={{ mb: 4, textAlign: 'center' }}>
                <Typography component="h1" variant="h4" fontWeight={700} color="text.primary">
                    Welcome Back
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    Please enter your details to sign in
                </Typography>
            </Box>

            <Box sx={{ mt: 6, mb: 4, transition: 'transform 0.2s ease', '&:hover': { transform: 'translateY(-1px)' } }}>
                <Button
                    fullWidth
                    variant="contained"
                    size="large"
                    onClick={() => {
                        window.location.href = 'http://localhost:8080/oauth2/authorization/google';
                    }}
                    sx={{
                        py: 1.5,
                        borderRadius: '14px',
                        fontWeight: 'bold',
                        fontSize: '1rem',
                        textTransform: 'none',
                        boxShadow: `0 10px 22px ${alpha(theme.palette.primary.main, 0.35)}`,
                        background: `linear-gradient(135deg, ${theme.palette.primary.main} 0%, ${theme.palette.primary.dark} 100%)`,
                        '&:hover': {
                            boxShadow: `0 14px 28px ${alpha(theme.palette.primary.main, 0.42)}`,
                        },
                    }}
                >
                    Sign In with Google <LoginIcon sx={{ ml: 1, fontSize: 20 }} />
                </Button>
            </Box>

            <Box sx={{ mt: 3, textAlign: 'center' }}>
                <Typography variant="body2" color="text.secondary">
                    Registration is handled via Google.
                </Typography>
            </Box>
        </Box>
    );
};

export default SignInForm;
