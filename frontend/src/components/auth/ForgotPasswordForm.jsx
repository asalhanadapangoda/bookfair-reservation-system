import React from 'react';
import {
    Box,
    Button,
    TextField,
    Typography,
    Link,
    InputAdornment,
    CircularProgress,
    useTheme,
    alpha,
    Stack
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { Email, ArrowBack, CheckCircleOutline, Lock, Visibility, VisibilityOff, VpnKey } from '@mui/icons-material';
import { IconButton } from '@mui/material';

const ForgotPasswordForm = ({
    step,
    email,
    otp,
    passwords,
    handleChange,
    handleSubmit,
    error,
    loading
}) => {
    const theme = useTheme();
    const [showPassword, setShowPassword] = React.useState(false);

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

    if (step === 4) {
        return (
            <Box sx={{ textAlign: 'center', py: 4, width: '100%' }}>
                <Box
                    sx={{
                        animation: 'popIn 360ms ease-out',
                        '@keyframes popIn': {
                            from: { opacity: 0, transform: 'scale(0.85)' },
                            to: { opacity: 1, transform: 'scale(1)' },
                        },
                    }}
                >
                    <CheckCircleOutline sx={{ mb: 2, fontSize: 56, color: theme.palette.success.main }} />
                </Box>
                <Typography variant="h5" fontWeight="bold" color="text.primary" gutterBottom>
                    Password Reset!
                </Typography>
                <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
                    Your password has been successfully updated.
                </Typography>
                <Button
                    component={RouterLink}
                    to="/login"
                    variant="contained"
                    size="large"
                    sx={{
                        borderRadius: '14px',
                        textTransform: 'none',
                        fontWeight: 'bold',
                        px: 4,
                        py: 1.5,
                        boxShadow: `0 10px 20px ${alpha(theme.palette.primary.main, 0.2)}`
                    }}
                >
                    Sign In Now
                </Button>
            </Box>
        );
    }

    return (
        <Box component="form" noValidate onSubmit={handleSubmit} sx={{ mt: 1, width: '100%' }}>
            <Box sx={{ mb: 4, textAlign: 'center' }}>
                <Typography component="h1" variant="h4" fontWeight={700} color="text.primary">
                    {step === 1 ? "Forgot Password" : step === 2 ? "Verify OTP" : "Reset Password"}
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    {step === 1 ? "Enter your email to receive an OTP" : step === 2 ? `Enter the OTP sent to ${email}` : `Set a new password for ${email}`}
                </Typography>
            </Box>

            {step === 1 && (
                <TextField
                    margin="normal"
                    required
                    fullWidth
                    id="email"
                    label="Email Address"
                    name="email"
                    autoComplete="email"
                    autoFocus
                    value={email}
                    onChange={handleChange}
                    error={!!error}
                    helperText={error}
                    InputProps={{
                        startAdornment: (
                            <InputAdornment position="start">
                                <Email color="action" />
                            </InputAdornment>
                        ),
                    }}
                    sx={inputSx}
                />
            )}
            {step === 2 && (
                <TextField
                    margin="normal"
                    required
                    fullWidth
                    id="otp"
                    label="6-Digit OTP"
                    name="otp"
                    autoComplete="off"
                    autoFocus
                    value={otp}
                    onChange={handleChange}
                    error={!!error}
                    helperText={error}
                    inputProps={{ maxLength: 6 }}
                    InputProps={{
                        startAdornment: (
                            <InputAdornment position="start">
                                <VpnKey color="action" />
                            </InputAdornment>
                        ),
                    }}
                    sx={inputSx}
                />
            )}
            {step === 3 && (
                <Stack spacing={1}>
                    <TextField
                        margin="normal"
                        required
                        fullWidth
                        name="password"
                        label="New Password"
                        type={showPassword ? 'text' : 'password'}
                        id="password"
                        autoFocus
                        value={passwords.password}
                        onChange={handleChange}
                        error={!!error && error.includes('match') === false}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <Lock color="action" />
                                </InputAdornment>
                            ),
                            endAdornment: (
                                <InputAdornment position="end">
                                    <IconButton onClick={() => setShowPassword(!showPassword)}>
                                        {showPassword ? <VisibilityOff /> : <Visibility />}
                                    </IconButton>
                                </InputAdornment>
                            )
                        }}
                        sx={inputSx}
                    />
                    <TextField
                        margin="normal"
                        required
                        fullWidth
                        name="confirmPassword"
                        label="Confirm Password"
                        type={showPassword ? 'text' : 'password'}
                        id="confirmPassword"
                        value={passwords.confirmPassword}
                        onChange={handleChange}
                        error={!!error && error.includes('match')}
                        helperText={error}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <Lock color="action" />
                                </InputAdornment>
                            ),
                        }}
                        sx={inputSx}
                    />
                </Stack>
            )}

            <Box sx={{ transition: 'transform 0.2s ease', '&:hover': { transform: 'translateY(-1px)' } }}>
                <Button
                    type="submit"
                    fullWidth
                    variant="contained"
                    size="large"
                    disabled={loading}
                    sx={{
                        mt: 3,
                        py: 1.5,
                        borderRadius: '14px',
                        fontWeight: 'bold',
                        textTransform: 'none',
                        fontSize: '1rem',
                        boxShadow: `0 10px 22px ${alpha(theme.palette.primary.main, 0.35)}`,
                        background: `linear-gradient(135deg, ${theme.palette.primary.main} 0%, ${theme.palette.primary.dark} 100%)`,
                        '&:hover': {
                            boxShadow: `0 14px 28px ${alpha(theme.palette.primary.main, 0.42)}`,
                        },
                    }}
                >
                    {loading ? <CircularProgress size={24} color="inherit" /> : (step === 1 ? 'Send OTP' : step === 2 ? 'Verify OTP' : 'Update Password')}
                </Button>
            </Box>

            <Box sx={{ mt: 3, textAlign: 'center' }}>
                <Link
                    component={RouterLink}
                    to="/login"
                    variant="subtitle2"
                    underline="hover"
                    sx={{
                        color: theme.palette.secondary.main,
                        fontWeight: 'bold',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 0.5
                    }}
                >
                    <ArrowBack sx={{ fontSize: 16 }} />
                    Back to Login
                </Link>
            </Box>
        </Box>
    );
};

export default ForgotPasswordForm;
