import React, { useEffect } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  CircularProgress,
  Typography,
  Stack,
  Alert
} from "@mui/material";

const AIModelDefinitionDialog = ({ open, onClose, definition, loading, error }) => {
  useEffect(() => {
    // Reset the form when the dialog is closed or definition is updated
    if (!open) {
      // You can reset any state here if needed
    }
  }, [open]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>AI Model Definition</DialogTitle>
      <DialogContent>
        {loading && (
          <Stack alignItems="center" justifyContent="center">
            <CircularProgress />
            <Typography variant="body1" sx={{ mt: 2 }}>
              Loading details...
            </Typography>
          </Stack>
        )}

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {definition && !loading && !error && (
          <Stack spacing={2}>
            <TextField
              label="Model Name"
              fullWidth
              value={definition.modelName || ""}
              InputProps={{
                readOnly: true,
              }}
            />
            <TextField
              label="Description"
              fullWidth
              value={definition.modelDescription || ""}
              InputProps={{
                readOnly: true,
              }}
            />
            <TextField
              label="Version"
              fullWidth
              value={definition.version || ""}
              InputProps={{
                readOnly: true,
              }}
            />
            <TextField
              label="Model Type"
              fullWidth
              value={definition.modelType || ""}
              InputProps={{
                readOnly: true,
              }}
            />
            <TextField
              label="Last Updated"
              fullWidth
              value={new Date(definition.updatedTs).toLocaleString()}
              InputProps={{
                readOnly: true,
              }}
            />
          </Stack>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="secondary">
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default AIModelDefinitionDialog;
