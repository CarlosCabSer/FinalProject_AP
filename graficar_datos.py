import pandas as pd
import matplotlib.pyplot as plt
from datetime import datetime
import sys
import os

def convertir_yearmonth(yearmonth_str):
    """
    Convierte un string en formato AAAAMM (ej: '198801') a formato de fecha.
    """
    try:
        # Convertir a string y limpiar espacios
        yearmonth_str = str(yearmonth_str).strip()

        # Si tiene menos de 6 dígitos, es inválido
        if len(yearmonth_str) < 6:
            return None

        year = int(yearmonth_str[:4])
        month = int(yearmonth_str[4:6])

        # Validar mes
        if month < 1 or month > 12:
            return None

        return datetime(year, month, 1)
    except Exception as e:
        print(f"Error convirtiendo '{yearmonth_str}': {e}")
        return None

def graficar_csv(ruta_csv):
    """
    Lee un archivo CSV y genera gráficos basados en la columna yearmonth (primera columna).
    """
    # Verificar que el archivo existe
    if not os.path.exists(ruta_csv):
        print(f"Error: El archivo '{ruta_csv}' no existe.")
        return

    # Leer el archivo CSV
    print(f"Leyendo archivo: {ruta_csv}")
    try:
        # Intentar leer con headers
        df = pd.read_csv(ruta_csv)

        # Verificar si la primera fila parece ser datos en lugar de headers
        primera_columna = df.columns[0]

        # Si la primera columna parece ser un número (yearmonth), no hay headers
        try:
            int(str(primera_columna))
            # Si llegamos aquí, la primera columna es un número, no hay headers
            print("Detectado: archivo sin encabezados")
            df = pd.read_csv(ruta_csv, header=None,
                           names=['yearmonth', 'exp_imp', 'Country', 'Custom', 'hs9', 'Q1', 'Q2', 'Value'])
        except ValueError:
            # Hay headers normales
            print("Detectado: archivo con encabezados")

    except Exception as e:
        print(f"Error al leer el archivo CSV: {e}")
        return

    # Verificar que el archivo no esté vacío
    if df.empty:
        print("Error: El archivo CSV está vacío.")
        return

    print(f"Columnas encontradas: {list(df.columns)}")
    print(f"Total de registros: {len(df)}")

    # Obtener la primera columna (yearmonth)
    columna_tiempo = df.columns[0]

    # Mostrar algunos ejemplos de la columna de tiempo
    print(f"\nPrimeros valores de '{columna_tiempo}':")
    print(df[columna_tiempo].head(10))

    # Convertir yearmonth a formato de fecha
    print("\nConvirtiendo fechas...")
    df['fecha'] = df[columna_tiempo].apply(convertir_yearmonth)

    # Mostrar cuántas fechas se convirtieron correctamente
    fechas_validas = df['fecha'].notna().sum()
    print(f"Fechas convertidas correctamente: {fechas_validas} de {len(df)}")

    # Eliminar filas con fechas inválidas
    df_original_len = len(df)
    df = df.dropna(subset=['fecha'])

    if df.empty:
        print("Error: No se pudieron convertir las fechas correctamente.")
        print("Verifique que la primera columna tenga el formato AAAAMM (ej: 198801)")
        return

    print(f"Registros válidos para graficar: {len(df)} (se eliminaron {df_original_len - len(df)} registros)")

    # Ordenar por fecha
    df = df.sort_values('fecha')

    print(f"\nRango de fechas: {df['fecha'].min().strftime('%Y-%m')} a {df['fecha'].max().strftime('%Y-%m')}")

    # Crear visualizaciones
    columnas_numericas = df.select_dtypes(include=['int64', 'float64']).columns.tolist()
    # Excluir la columna de tiempo original
    columnas_numericas = [col for col in columnas_numericas if col != columna_tiempo]

    if len(columnas_numericas) == 0:
        print("\nNo se encontraron columnas numéricas para graficar.")
        print("Intentando convertir columnas a numéricas...")

        # Intentar convertir columnas a numéricas
        for col in df.columns:
            if col not in [columna_tiempo, 'fecha']:
                try:
                    df[col] = pd.to_numeric(df[col], errors='coerce')
                    if df[col].notna().sum() > 0:
                        columnas_numericas.append(col)
                except:
                    pass

        if len(columnas_numericas) == 0:
            print("No se pudieron encontrar columnas numéricas.")
            return

    print(f"\nColumnas numéricas disponibles: {columnas_numericas}")

    # Opción 1: Graficar todas las columnas numéricas en subplots
    num_columnas = len(columnas_numericas)

    print(f"\nGenerando gráficos para {num_columnas} columnas...")

    if num_columnas <= 6:
        # Si hay pocas columnas, crear un subplot por cada una
        fig, axes = plt.subplots(num_columnas, 1, figsize=(14, 4*num_columnas))
        if num_columnas == 1:
            axes = [axes]

        for i, columna in enumerate(columnas_numericas):
            # Agrupar por fecha y sumar
            df_agrupado = df.groupby('fecha')[columna].sum().reset_index()

            axes[i].plot(df_agrupado['fecha'], df_agrupado[columna],
                        marker='o', linestyle='-', markersize=4, linewidth=2)
            axes[i].set_xlabel('Fecha (AAAA-MM)', fontsize=10)
            axes[i].set_ylabel(columna, fontsize=10)
            axes[i].set_title(f'{columna} a lo largo del tiempo', fontsize=12, fontweight='bold')
            axes[i].grid(True, alpha=0.3)
            axes[i].tick_params(axis='x', rotation=45)

        plt.tight_layout()
        nombre_salida = ruta_csv.replace('.csv', '_graficos_separados.png')
        plt.savefig(nombre_salida, dpi=300, bbox_inches='tight')
        print(f"\n✓ Gráfico guardado como: {nombre_salida}")
        plt.show()
        plt.close()

    # Opción 2: Gráfico combinado (si hay columna 'Value')
    if 'Value' in df.columns:
        print("\nGenerando gráfico de valores totales...")
        plt.figure(figsize=(14, 6))

        # Agrupar por fecha para sumar valores
        df_agrupado = df.groupby('fecha')['Value'].sum().reset_index()

        plt.plot(df_agrupado['fecha'], df_agrupado['Value'],
                marker='o', linestyle='-', linewidth=2, markersize=5, color='steelblue')
        plt.xlabel('Fecha (AAAA-MM)', fontsize=12)
        plt.ylabel('Valor Total', fontsize=12)
        plt.title('Evolución del Valor Total a lo largo del tiempo', fontsize=14, fontweight='bold')
        plt.grid(True, alpha=0.3)
        plt.xticks(rotation=45)
        plt.tight_layout()

        nombre_salida = ruta_csv.replace('.csv', '_valor_total.png')
        plt.savefig(nombre_salida, dpi=300, bbox_inches='tight')
        print(f"✓ Gráfico de valor total guardado como: {nombre_salida}")
        plt.show()
        plt.close()

    # Opción 3: Si hay columnas Q1 y Q2, graficarlas juntas
    if 'Q1' in df.columns and 'Q2' in df.columns:
        print("\nGenerando gráfico de cantidades (Q1 y Q2)...")
        plt.figure(figsize=(14, 6))

        df_agrupado = df.groupby('fecha')[['Q1', 'Q2']].sum().reset_index()

        plt.plot(df_agrupado['fecha'], df_agrupado['Q1'],
                marker='o', linestyle='-', linewidth=2, markersize=5,
                label='Q1', color='coral')
        plt.plot(df_agrupado['fecha'], df_agrupado['Q2'],
                marker='s', linestyle='-', linewidth=2, markersize=5,
                label='Q2', color='seagreen')

        plt.xlabel('Fecha (AAAA-MM)', fontsize=12)
        plt.ylabel('Cantidad', fontsize=12)
        plt.title('Evolución de Q1 y Q2 a lo largo del tiempo', fontsize=14, fontweight='bold')
        plt.legend(fontsize=11)
        plt.grid(True, alpha=0.3)
        plt.xticks(rotation=45)
        plt.tight_layout()

        nombre_salida = ruta_csv.replace('.csv', '_cantidades.png')
        plt.savefig(nombre_salida, dpi=300, bbox_inches='tight')
        print(f"✓ Gráfico de cantidades guardado como: {nombre_salida}")
        plt.show()
        plt.close()

def main():
    """
    Función principal del script.
    """
    print("=" * 60)
    print(" SCRIPT DE VISUALIZACIÓN DE DATOS CSV - ANÁLISIS TEMPORAL")
    print("=" * 60)

    # Solicitar ruta del archivo si no se proporciona como argumento
    if len(sys.argv) > 1:
        ruta_csv = sys.argv[1]
    else:
        ruta_csv = input("\nIngrese la ruta del archivo CSV: ").strip().replace('"', '')

    # Graficar los datos
    graficar_csv(ruta_csv)

    print("\n" + "=" * 60)
    print(" Proceso completado")
    print("=" * 60)

if __name__ == "__main__":
    main()
