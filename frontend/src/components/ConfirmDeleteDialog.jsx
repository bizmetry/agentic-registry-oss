import React from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Stack,
  Alert
} from "@mui/material";

export default function ConfirmDeleteDialog({
  open,
  title = "Delete",
  message,
  confirmText = "Delete",
  cancelText = "Cancel",
  loading = false,
  error = null,
  onClose,
  onConfirm
}) {
  return (
    <Dialog open={open} onClose={loading ? undefined : onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ fontWeight: 900 }}>{title}</DialogTitle>

      <DialogContent>
        <Stack spacing={2} sx={{ pt: 0.5 }}>
          {message ? (
            <Typography variant="body2" sx={{ opacity: 0.85, whiteSpace: "pre-wrap" }}>
              {message}
            </Typography>
          ) : null}

          {error ? <Alert severity="error">{error}</Alert> : null}
        </Stack>
      </DialogContent>

      <DialogActions sx={{ px: 2.5, pb: 2 }}>
        <Button onClick={onClose} color="inherit" disabled={loading}>
          {cancelText}
        </Button>

        <Button
          onClick={onConfirm}
          variant="contained"
          color="error"
          disabled={loading}
        >
          {loading ? "Deleting..." : confirmText}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
