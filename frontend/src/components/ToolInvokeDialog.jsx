import React, { useEffect, useMemo, useState } from "react";
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, TextField, MenuItem, Alert, Stack, Typography,
  Divider, Paper, IconButton,
  Accordion, AccordionSummary, AccordionDetails,
  InputAdornment, FormControl, InputLabel, Select
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import SecurityIcon from "@mui/icons-material/Security";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import TimerOutlinedIcon from "@mui/icons-material/TimerOutlined";

const isValidBearerToken = (token) => {
  if (!token) return true; // optional
  const t = token.trim();
  const clean = t.toLowerCase().startsWith("bearer ") ? t.slice(7).trim() : t;

  // JWT format: header.payload.signature (base64url-ish)
  const jwtRegex = /^[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+$/;
  return jwtRegex.test(clean);
};

export default function ToolInvokeDialog({ open, onClose, tool, server, onRun }) {
  const schema = useMemo(() => {
    const a = tool?.arguments;
    if (!a) return null;
    if (typeof a === "string") {
      try { return JSON.parse(a); } catch { return null; }
    }
    return typeof a === "object" ? a : null;
  }, [tool]);

  const props = schema?.properties && typeof schema.properties === "object" ? schema.properties : {};
  const required = new Set(Array.isArray(schema?.required) ? schema.required : []);

  // ✅ Security
  const [authType, setAuthType] = useState("NONE"); // NONE | BEARER
  const [bearerToken, setBearerToken] = useState("");
  const [bearerError, setBearerError] = useState("");
  const [showToken, setShowToken] = useState(false);

  // ✅ Request
  const [timeoutMs, setTimeoutMs] = useState(30000);

  const [values, setValues] = useState({});
  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (!open) return;

    const init = {};
    Object.entries(props).forEach(([k, def]) => {
      if (def && Object.prototype.hasOwnProperty.call(def, "default")) init[k] = def.default;
      else init[k] = "";
    });

    setValues(init);
    setErrors({});

    setAuthType("NONE");
    setBearerToken("");
    setBearerError("");
    setShowToken(false);

    setTimeoutMs(30000);
  }, [open, tool, schema]); // eslint-disable-line

  const setField = (name, val) => {
    setValues(v => ({ ...v, [name]: val }));
    setErrors(e => {
      const copy = { ...e };
      delete copy[name];
      return copy;
    });
  };

  const coerceValue = (def, raw) => {
    const type = def?.type;

    if (type === "boolean") return !!raw;

    if (type === "integer") {
      if (raw === "" || raw == null) return raw;
      const n = Number(raw);
      return Number.isFinite(n) ? Math.trunc(n) : raw;
    }

    if (type === "number") {
      if (raw === "" || raw == null) return raw;
      const n = Number(raw);
      return Number.isFinite(n) ? n : raw;
    }

    if (type === "array") {
      if (typeof raw !== "string") return raw;
      const s = raw.trim();
      if (!s) return [];
      try {
        const j = JSON.parse(s);
        return Array.isArray(j) ? j : [j];
      } catch {
        return s.split(",").map(x => x.trim()).filter(Boolean);
      }
    }

    if (type === "object") {
      if (typeof raw !== "string") return raw;
      const s = raw.trim();
      if (!s) return {};
      try {
        const j = JSON.parse(s);
        return (j && typeof j === "object") ? j : {};
      } catch {
        return raw; // señal de error posible
      }
    }

    return raw;
  };

  const validate = () => {
    const next = {};
    Object.entries(props).forEach(([name, def]) => {
      const v = values[name];
      if (!required.has(name)) return;

      if (def?.type === "boolean") return;

      if (def?.type === "array") {
        if (!v || (typeof v === "string" && v.trim() === "")) next[name] = "Required";
        return;
      }

      if (def?.type === "object") {
        if (v == null || String(v).trim() === "") next[name] = "Required (JSON)";
        return;
      }

      if (v == null || String(v).trim() === "") next[name] = "Required";
    });

    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const normalizeBearer = (raw) => {
    const t = String(raw || "").trim();
    if (!t) return "";
    return t.toLowerCase().startsWith("bearer ") ? t.slice(7).trim() : t;
  };

  const run = () => {
    if (!tool) return;

    // ✅ bloquear si bearer inválido
    if (authType === "BEARER" && bearerError) return;

    if (!validate()) return;

    const args = {};
    Object.entries(props).forEach(([name, def]) => {
      const raw = values[name];
      const isReq = required.has(name);

      const isEmpty =
        raw == null ||
        (typeof raw === "string" && raw.trim() === "") ||
        (Array.isArray(raw) && raw.length === 0);

      if (!isReq && isEmpty) return;

      args[name] = coerceValue(def, raw);
    });

    const token = authType === "BEARER" ? normalizeBearer(bearerToken) : "";

    const payload = {
      server: {
        name: server?.name ?? null,
        discoveryUrl: server?.discoveryUrl ?? null,
        resolvedUrl: server?.resolvedUrl ?? null,
        version: server?.version ?? null
      },
      tool: {
        name: tool?.name ?? null,
        description: tool?.description ?? null
      },
      args,
      timeoutMs: Number(timeoutMs) || 30000,
      auth: token ? { bearerToken: token } : undefined
    };

    onRun?.(payload);
    onClose?.();
  };

  const renderField = (name, def) => {
    const type = def?.type || "string";
    const label = `${name}${required.has(name) ? " *" : ""}`;
    const helper = def?.description || def?.title || "";

    if (Array.isArray(def?.enum) && def.enum.length > 0) {
      return (
        <TextField
          key={name}
          select
          fullWidth
          size="small"
          label={label}
          value={values[name] ?? ""}
          onChange={(e) => setField(name, e.target.value)}
          error={!!errors[name]}
          helperText={errors[name] || helper}
        >
          {def.enum.map(opt => (
            <MenuItem key={String(opt)} value={opt}>{String(opt)}</MenuItem>
          ))}
        </TextField>
      );
    }

    if (type === "boolean") {
      return (
        <TextField
          key={name}
          select
          fullWidth
          size="small"
          label={label}
          value={String(values[name] ?? false)}
          onChange={(e) => setField(name, e.target.value === "true")}
          error={!!errors[name]}
          helperText={errors[name] || helper || "true / false"}
        >
          <MenuItem value="true">true</MenuItem>
          <MenuItem value="false">false</MenuItem>
        </TextField>
      );
    }

    if (type === "number" || type === "integer") {
      return (
        <TextField
          key={name}
          fullWidth
          size="small"
          type="number"
          label={label}
          value={values[name] ?? ""}
          onChange={(e) => setField(name, e.target.value)}
          error={!!errors[name]}
          helperText={errors[name] || helper}
        />
      );
    }

    if (type === "array") {
      return (
        <TextField
          key={name}
          fullWidth
          size="small"
          label={label}
          value={values[name] ?? ""}
          onChange={(e) => setField(name, e.target.value)}
          error={!!errors[name]}
          helperText={errors[name] || helper || "JSON array (preferred) or comma-separated"}
          multiline
          minRows={2}
        />
      );
    }

    if (type === "object") {
      return (
        <TextField
          key={name}
          fullWidth
          size="small"
          label={label}
          value={values[name] ?? ""}
          onChange={(e) => setField(name, e.target.value)}
          error={!!errors[name]}
          helperText={errors[name] || helper || "JSON object"}
          multiline
          minRows={3}
        />
      );
    }

    return (
      <TextField
        key={name}
        fullWidth
        size="small"
        label={label}
        value={values[name] ?? ""}
        onChange={(e) => setField(name, e.target.value)}
        error={!!errors[name]}
        helperText={errors[name] || helper}
      />
    );
  };

  const hasArgs = Object.keys(props).length > 0;

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle sx={{ pb: 1 }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between">
          <Stack spacing={0.25}>
            <Typography fontWeight={900}>Invoke Tool</Typography>
            <Typography variant="body2" sx={{ opacity: 0.8, fontFamily: "monospace" }}>
              {tool?.name || "—"}
            </Typography>
          </Stack>

          <IconButton onClick={onClose}>
            <CloseIcon fontSize="small" />
          </IconButton>
        </Stack>
      </DialogTitle>

      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
            <Typography variant="body2" sx={{ opacity: 0.85 }}>
              {tool?.description || "—"}
            </Typography>
            <Divider sx={{ my: 1 }} />
            <Typography variant="caption" sx={{ opacity: 0.8 }}>
              Server: <b>{server?.resolvedUrl || server?.discoveryUrl || "—"}</b>
            </Typography>
          </Paper>

          {/* ✅ Request (timeout separado) */}
          <Accordion defaultExpanded>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Stack direction="row" spacing={1} alignItems="center">
                <TimerOutlinedIcon fontSize="small" />
                <Typography fontWeight={800}>Request</Typography>
              </Stack>
            </AccordionSummary>
            <AccordionDetails>
              <TextField
                label="Timeout (ms)"
                type="number"
                value={timeoutMs}
                onChange={(e) => setTimeoutMs(Number(e.target.value || 30000))}
                fullWidth
                size="small"
                margin="dense"
              />
            </AccordionDetails>
          </Accordion>

          {/* ✅ Security (bearer separado + validación) */}
          <Accordion>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Stack direction="row" spacing={1} alignItems="center">
                <SecurityIcon fontSize="small" />
                <Typography fontWeight={800}>Security</Typography>
              </Stack>
            </AccordionSummary>
            <AccordionDetails>
              <Stack spacing={1.25}>
                <FormControl fullWidth size="small">
                  <InputLabel id="auth-type-label">Auth</InputLabel>
                  <Select
                    labelId="auth-type-label"
                    label="Auth"
                    value={authType}
                    onChange={(e) => {
                      const v = e.target.value;
                      setAuthType(v);
                      if (v !== "BEARER") {
                        setBearerError("");
                      } else {
                        // validate current token when switching to BEARER
                        const ok = isValidBearerToken(bearerToken);
                        setBearerError(ok ? "" : "Invalid token format (expected JWT: header.payload.signature)");
                      }
                    }}
                  >
                    <MenuItem value="NONE">None</MenuItem>
                    <MenuItem value="BEARER">Bearer token</MenuItem>
                  </Select>
                </FormControl>

                {authType === "BEARER" && (
                  <TextField
                    label="Bearer Token"
                    value={bearerToken}
                    onChange={(e) => {
                      const v = e.target.value;
                      setBearerToken(v);
                      const ok = isValidBearerToken(v);
                      setBearerError(ok ? "" : "Invalid token format (expected JWT: header.payload.signature)");
                    }}
                    placeholder="Paste JWT (with or without 'Bearer ')"
                    fullWidth
                    size="small"
                    margin="dense"
                    type={showToken ? "text" : "password"}
                    error={!!bearerError}
                    helperText={bearerError || "JWT format: header.payload.signature"}
                    InputProps={{
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton onClick={() => setShowToken(s => !s)} edge="end">
                            {showToken ? <VisibilityOffIcon /> : <VisibilityIcon />}
                          </IconButton>
                        </InputAdornment>
                      )
                    }}
                  />
                )}
              </Stack>
            </AccordionDetails>
          </Accordion>

          {!hasArgs ? (
            <Alert severity="info" variant="outlined">
              This tool has no arguments.
            </Alert>
          ) : (
            <Stack spacing={1.5}>
              {Object.entries(props).map(([n, def]) => renderField(n, def))}
            </Stack>
          )}

           </Stack>
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          variant="contained"
          onClick={run}
          sx={{ fontWeight: 900 }}
          disabled={authType === "BEARER" && !!bearerError}
        >
          Run
        </Button>
      </DialogActions>
    </Dialog>
  );
}
