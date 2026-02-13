import React, { useState, useEffect, useMemo } from "react";
import {
  listServers,
  createServer,
  updateServer,
  deleteServer,
  testConnection
} from "../../api/mcpServers";
import { safe } from "../../api/client";
import {
  Grid,
  Button,
  Pagination,
  Stack,
  TextField,
  MenuItem,
  InputAdornment,
  CircularProgress
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import McpServerCard from "../../components/McpServerCard";
import McpServerForm from "./McpServerForm";
import { ImportMcpServerDialog } from "../../components/ImportMcpServerDialog"; // Importar el componente para el diálogo

const PAGE_SIZE = 8;

// ===============================
// Sorting options
// ===============================
const SORT_OPTIONS = [
  { label: "Name (A → Z)", sortBy: "name", sortDir: "asc" },
  { label: "Name (Z → A)", sortBy: "name", sortDir: "desc" },
  { label: "Created (Oldest)", sortBy: "timestamp", sortDir: "asc" },
  { label: "Created (Newest)", sortBy: "timestamp", sortDir: "desc" }
];

// Debounce delay (ms)
const SEARCH_DELAY = 500;

export default function McpServersView() {

  const [items, setItems] = useState([]);
  const [page, setPage] = useState(1);
  const [open, setOpen] = useState(false);
  const [edit, setEdit] = useState(null);

  // ===============================
  // Search + Sorting state
  // ===============================
  const [searchInput, setSearchInput] = useState("");
  const [searchQuery, setSearchQuery] = useState("");

  const [sortValue, setSortValue] = useState(SORT_OPTIONS[3]); // default newest first
  const [loading, setLoading] = useState(false);

  // ===============================
  // Debounce search (type-ahead)
  // ===============================
  useEffect(() => {
    const t = setTimeout(() => {
      setSearchQuery(searchInput.trim());
      setPage(1); // reset pagination
    }, SEARCH_DELAY);

    return () => clearTimeout(t);
  }, [searchInput]);

  // ===============================
  // Load from backend
  // ===============================
  const load = async () => {
    setLoading(true);

    const params = {
      q: searchQuery || undefined,
      sortBy: sortValue.sortBy,
      sortDir: sortValue.sortDir
    };

    const r = await safe(() => listServers(params));

    if (r.ok) {
      setItems(r.data || []);
    }

    setLoading(false);
  };

  useEffect(() => {
    load();
  }, [searchQuery, sortValue]);

  // ===============================
  // Pagination slice
  // ===============================
  const slice = useMemo(() => {
    return items.slice(
      (page - 1) * PAGE_SIZE,
      page * PAGE_SIZE
    );
  }, [items, page]);

  // ===============================
  // UI
  // ===============================
  const [importDialogOpen, setImportDialogOpen] = useState(false);

  // Función para manejar cuando los datos se importan correctamente
  const onImported = (res) => {
    console.log("Import result:", res);
    load();  // Refrescar la lista de servidores luego de importar
  };

  return (
    <div style={{ padding: 20 }}>

      {/* ========================= */}
      {/* Top controls */}
      {/* ========================= */}
      <Stack
        direction="row" // Poner en fila los botones
        spacing={2} // Separación entre los botones
        alignItems="center"
        sx={{ mb: 3 }} // Margen inferior para separar del contenido
      >

        <Button
          variant="contained"
          onClick={() => {
            setEdit(null);
            setOpen(true);
          }}
          sx={{ fontWeight: 900, padding: "8px 16px" }}
        >
          + MCP Server
        </Button>

        {/* Botón Import MCP Server */}
        <Button
          variant="outlined"
          onClick={() => setImportDialogOpen(true)}
          sx={{ fontWeight: 900, padding: "8px 16px" }}
        >
          Import MCP Server
        </Button>

      </Stack>

      {/* ========================= */}
      {/* Search and sorting controls */}
      {/* ========================= */}
      <Stack
        direction={{ xs: "column", sm: "row" }}
        spacing={1}
        sx={{ minWidth: 420 }}
      >

        {/* 🔍 Search bar */}
        <TextField
          fullWidth
          placeholder="Search server or tool..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon sx={{ opacity: 0.6 }} />
              </InputAdornment>
            ),
            endAdornment: loading ? (
              <InputAdornment position="end">
                <CircularProgress size={18} />
              </InputAdornment>
            ) : null
          }}
        />

        {/* ⬇️ Sorting dropdown */}
        <TextField
          select
          label="Sort by"
          value={SORT_OPTIONS.indexOf(sortValue)}
          onChange={(e) => {
            setSortValue(SORT_OPTIONS[e.target.value]);
            setPage(1);
          }}
          sx={{ minWidth: 200 }}
        >
          {SORT_OPTIONS.map((opt, idx) => (
            <MenuItem key={idx} value={idx}>
              {opt.label}
            </MenuItem>
          ))}
        </TextField>

      </Stack>

      {/* ========================= */}
      {/* Cards grid */}
      {/* ========================= */}
      <Grid container spacing={2} sx={{ mt: 2 }}>

        {slice.map(s => (
          <Grid item xs={12} sm={6} md={3} key={s.serverId || s.id}>
            <McpServerCard
              s={s}
              onEdit={() => {
                setEdit(s);
                setOpen(true);
              }}
              onDelete={async () => {
                await deleteServer(s.serverId || s.id);
                load();
              }}
            />
          </Grid>
        ))}

        {!loading && slice.length === 0 && (
          <Grid item xs={12} sx={{ opacity: 0.7, textAlign: "center", mt: 4 }}>
            No MCP Servers found
          </Grid>
        )}
      </Grid>

      {/* ========================= */}
      {/* Pagination */}
      {/* ========================= */}
      {items.length > PAGE_SIZE && (
        <Pagination
          sx={{ mt: 3 }}
          count={Math.ceil(items.length / PAGE_SIZE)}
          page={page}
          onChange={(_, v) => setPage(v)}
        />
      )}

      {/* ========================= */}
      {/* Form dialog */}
      {/* ========================= */}
      <McpServerForm
        open={open}
        initial={edit}
        onClose={() => setOpen(false)}
        onSave={async (p) => {
          if (edit) await updateServer(edit.serverId || edit.id, p);
          else await createServer(p);

          setOpen(false);
          load();
        }}
        onTest={testConnection}
      />

      {/* ========================= */}
      {/* Import Dialog */}
      {/* ========================= */}
      <ImportMcpServerDialog
        open={importDialogOpen}
        onClose={() => setImportDialogOpen(false)}
        onImported={onImported}
      />

    </div>
  );
}
